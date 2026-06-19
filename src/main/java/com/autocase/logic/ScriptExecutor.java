package com.autocase.logic;

import com.autocase.dao.CaseDao;
import com.autocase.dao.ScriptDao;
import com.autocase.entity.ExecutionResult;
import com.autocase.entity.ReportData;
import com.autocase.entity.TestCase;
import com.autocase.entity.TestScript;
import com.autocase.util.Constants;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 脚本执行引擎 - 负责并行执行测试脚本
 * 支持超时熔断、崩溃检测、基线快照、Debug日志收集
 */
public class ScriptExecutor {

    private final ScriptDao scriptDao;
    private final CaseDao caseDao;

    // 默认执行命令模板
    private final Map<String, String> defaultCommands;

    // 用户自定义执行命令
    private Map<String, String> customCommands;

    // 通过/失败判定规则
    private String passPattern = "PASS";
    private String failPattern = "FAIL";
    private String blockPattern = "BLOCK";

    // 执行超时（毫秒）
    private int executionTimeout = Constants.DEFAULT_EXECUTION_TIMEOUT;

    // Debug日志目录
    private final String debugLogDir;

    // 执行控制
    private ExecutorService executorService;
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean paused = new AtomicBoolean(false);
    private final Object pauseLock = new Object();

    // 进度回调
    private Runnable onProgressChanged;
    private Consumer<String> onStatusChanged;

    // 基线快照
    private Map<String, String> baselineSnapshot;

    public ScriptExecutor() {
        this.scriptDao = new ScriptDao();
        this.caseDao = new CaseDao();
        this.customCommands = new HashMap<>();

        // 初始化Debug日志目录
        String userHome = System.getProperty("user.home");
        this.debugLogDir = Paths.get(userHome, Constants.CONFIG_DIR, Constants.DEBUG_LOG_DIR).toString();

        // 初始化默认命令
        defaultCommands = new HashMap<>();
        defaultCommands.put("Python", "python \"{0}\"");
        defaultCommands.put("C", "gcc \"{0}\" -o \"{1}\" && \"{1}\"");
        defaultCommands.put("C++", "g++ \"{0}\" -o \"{1}\" && \"{1}\"");
        defaultCommands.put("GDScript", "godot --headless --script \"{0}\"");
        defaultCommands.put("C#", "dotnet run --project \"{0}\"");
        defaultCommands.put("Java", "java -cp \"{1}\" \"{2}\"");
        defaultCommands.put("JavaScript", "node \"{0}\"");
        defaultCommands.put("TypeScript", "npx ts-node \"{0}\"");
        defaultCommands.put("Go", "go run \"{0}\"");
        defaultCommands.put("Rust", "rustc \"{0}\" -o \"{1}\" && \"{1}\"");
        defaultCommands.put("Ruby", "ruby \"{0}\"");
        defaultCommands.put("Shell", "bash \"{0}\"");
        defaultCommands.put("Batch", "cmd /c \"{0}\"");
    }

    /**
     * 设置自定义执行命令
     */
    public void setCustomCommands(Map<String, String> commands) {
        this.customCommands = commands != null ? commands : new HashMap<>();
    }

    /**
     * 获取当前执行命令（自定义优先）
     */
    public String getCommand(String language) {
        if (customCommands.containsKey(language)) {
            return customCommands.get(language);
        }
        return defaultCommands.getOrDefault(language, "echo \"Unsupported language: " + language + "\"");
    }

    /**
     * 设置判定规则
     */
    public void setPatterns(String passPattern, String failPattern, String blockPattern) {
        this.passPattern = passPattern != null ? passPattern : "PASS";
        this.failPattern = failPattern != null ? failPattern : "FAIL";
        this.blockPattern = blockPattern != null ? blockPattern : "BLOCK";
    }

    /**
     * 设置执行超时
     */
    public void setExecutionTimeout(int timeoutMs) {
        this.executionTimeout = timeoutMs > 0 ? timeoutMs : Constants.DEFAULT_EXECUTION_TIMEOUT;
    }

    /**
     * 获取执行超时
     */
    public int getExecutionTimeout() {
        return executionTimeout;
    }

    /**
     * 扫描脚本
     */
    public List<TestScript> scanScripts(String directoryPath) {
        return scriptDao.scanScripts(directoryPath);
    }

    /**
     * 执行选中的脚本
     */
    public ReportData executeScripts(String directoryPath, List<TestScript> scripts) {
        ReportData report = new ReportData();
        report.setReportId(com.autocase.dao.ReportDao.generateReportId());
        report.setRootDirectory(directoryPath);
        report.setStartTime(System.currentTimeMillis());

        // 收集基线快照
        baselineSnapshot = collectBaseline();
        report.setBaselineSnapshot(baselineSnapshot);

        // 清理过期Debug日志
        cleanupDebugLogs();

        List<TestScript> selectedScripts = new ArrayList<>();
        for (TestScript script : scripts) {
            if (script.isSelected()) {
                // 检查关联用例状态，过滤阻塞和跳过的用例
                String casePath = findAssociatedCase(script, directoryPath);
                if (casePath != null) {
                    TestCase tc = caseDao.loadCase(casePath);
                    if (tc != null) {
                        com.autocase.entity.CaseStatus status = tc.getCurrentStatus();
                        if (status == com.autocase.entity.CaseStatus.SKIPPED) {
                            ExecutionResult result = new ExecutionResult(
                                    script.getFilePath(), script.getFileName(),
                                    script.getLanguage(), script.getCaseName());
                            result.setStatus("SKIPPED");
                            result.setTestCasePath(casePath);
                            result.setPriority(tc.getPriority());
                            result.setSeverity(tc.getSeverity());
                            report.getResults().add(result);
                            continue;
                        } else if (status == com.autocase.entity.CaseStatus.BLOCKED) {
                            ExecutionResult result = new ExecutionResult(
                                    script.getFilePath(), script.getFileName(),
                                    script.getLanguage(), script.getCaseName());
                            result.setStatus("BLOCKED");
                            result.setTestCasePath(casePath);
                            result.setPriority(tc.getPriority());
                            result.setSeverity(tc.getSeverity());
                            report.getResults().add(result);
                            continue;
                        }
                    }
                }
                selectedScripts.add(script);
            } else {
                ExecutionResult result = new ExecutionResult(
                        script.getFilePath(), script.getFileName(),
                        script.getLanguage(), script.getCaseName());
                result.setStatus("NOT_EXECUTED");
                report.getResults().add(result);
            }
        }

        // 排序：优先级高优先 -> 步骤数短优先
        selectedScripts.sort((a, b) -> {
            // 优先级排序（P0 > P1 > P2 > P3 > null）
            String pa = a.getPriority();
            String pb = b.getPriority();
            int priorityCompare = comparePriority(pa, pb);
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // 步骤数排序（短优先）
            return Integer.compare(a.getStepsCount(), b.getStepsCount());
        });

        running.set(true);
        paused.set(false);
        int threadCount = Math.min(selectedScripts.size(), Runtime.getRuntime().availableProcessors());
        executorService = Executors.newFixedThreadPool(threadCount);

        AtomicInteger completed = new AtomicInteger(0);
        int total = selectedScripts.size();

        List<Future<ExecutionResult>> futures = new ArrayList<>();
        for (TestScript script : selectedScripts) {
            if (!running.get()) break;

            Future<ExecutionResult> future = executorService.submit(() -> {
                waitForResume();
                if (!running.get()) {
                    ExecutionResult result = new ExecutionResult(
                            script.getFilePath(), script.getFileName(),
                            script.getLanguage(), script.getCaseName());
                    result.setStatus("NOT_EXECUTED");
                    return result;
                }
                return executeSingleScript(script, directoryPath);
            });
            futures.add(future);
        }

        // 收集结果
        for (Future<ExecutionResult> future : futures) {
            try {
                ExecutionResult result = future.get();
                report.getResults().add(result);
                completed.incrementAndGet();

                if (onProgressChanged != null) {
                    javafx.application.Platform.runLater(onProgressChanged);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();
        running.set(false);

        report.setEndTime(System.currentTimeMillis());
        report.calculateStats();

        // 自动更新关联用例状态
        updateCaseStatuses(report);

        return report;
    }

    /**
     * 执行单个脚本
     */
    private ExecutionResult executeSingleScript(TestScript script, String rootDirectory) {
        ExecutionResult result = new ExecutionResult(
                script.getFilePath(), script.getFileName(),
                script.getLanguage(), script.getCaseName());

        long startTime = System.currentTimeMillis();
        final Process[] processRef = new Process[1];

        try {
            String command = buildCommand(script, rootDirectory);
            ProcessBuilder pb = new ProcessBuilder();

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd", "/c", command);
            } else {
                pb.command("sh", "-c", command);
            }

            pb.directory(new File(rootDirectory));
            pb.redirectErrorStream(true);

            processRef[0] = pb.start();
            final Process process = processRef[0];
            StringBuilder output = new StringBuilder();

            // 读取输出
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException e) {
                    // ignore
                }
            });
            outputReader.setDaemon(true);
            outputReader.start();

            // 等待完成（带超时，轮询方式）
            long elapsed = 0;
            long checkInterval = 100; // 每100ms检查一次
            boolean finished = false;
            while (elapsed < executionTimeout) {
                if (!process.isAlive()) {
                    finished = true;
                    break;
                }
                try {
                    Thread.sleep(checkInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                elapsed += checkInterval;
            }

            if (!finished) {
                // 超时：强制终止
                process.destroyForcibly();
                result.setStatus("TIMEOUT");
                result.setErrorMessage("执行超时 (" + executionTimeout + "ms)");
                result.setExecutionTime(executionTimeout);
            } else {
                outputReader.join(1000);
                int exitCode = process.exitValue();
                long executionTime = System.currentTimeMillis() - startTime;

                result.setExecutionTime(executionTime);
                result.setActualOutput(truncateOutput(output.toString()));

                // 保存Debug日志
                saveDebugLog(script.getFileName(), output.toString());

                // 判定结果
                String outputStr = output.toString().toUpperCase();
                if (outputStr.contains(blockPattern.toUpperCase())) {
                    result.setStatus("BLOCKED");
                } else if (outputStr.contains(failPattern.toUpperCase()) || exitCode != 0) {
                    result.setStatus("FAILED");
                    result.setErrorMessage("Exit code: " + exitCode);
                } else if (outputStr.contains(passPattern.toUpperCase())) {
                    result.setStatus("PASSED");
                } else {
                    result.setStatus(exitCode == 0 ? "PASSED" : "FAILED");
                    if (exitCode != 0) {
                        result.setErrorMessage("Exit code: " + exitCode);
                    }
                }
            }

            // 查找关联用例
            String casePath = findAssociatedCase(script, rootDirectory);
            result.setTestCasePath(casePath);

            // 填充优先级和严重程度
            if (casePath != null) {
                TestCase tc = caseDao.loadCase(casePath);
                if (tc != null) {
                    result.setPriority(tc.getPriority());
                    result.setSeverity(tc.getSeverity());
                }
            }

        } catch (IOException e) {
            result.setStatus("BLOCKED");
            result.setErrorMessage("执行异常: " + e.getMessage());
            result.setExecutionTime(System.currentTimeMillis() - startTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.setStatus("NOT_EXECUTED");
            result.setExecutionTime(System.currentTimeMillis() - startTime);
        } finally {
            if (processRef[0] != null && processRef[0].isAlive()) {
                processRef[0].destroyForcibly();
            }
        }

        return result;
    }

    /**
     * 构建执行命令
     */
    private String buildCommand(TestScript script, String rootDirectory) {
        String language = script.getLanguage();
        String template = getCommand(language);
        String filePath = script.getFilePath();

        switch (language) {
            case "C":
            case "C++":
            case "Rust":
                String exeName = script.getFileName().substring(0, script.getFileName().lastIndexOf('.'));
                String exePath = Paths.get(rootDirectory, exeName + (System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "")).toString();
                return template.replace("{0}", filePath).replace("{1}", exePath);
            case "Java":
                return buildJavaCommand(script, rootDirectory, filePath);
            default:
                return template.replace("{0}", filePath);
        }
    }

    /**
     * 构建Java执行命令（先编译再运行）
     */
    private String buildJavaCommand(TestScript script, String rootDirectory, String filePath) {
        // 查找CMS类路径
        String cmsClasspath = findCmsClasspath(rootDirectory);
        String classpath = cmsClasspath != null ? cmsClasspath : ".";

        // 获取包名和类名
        String packageName = extractPackageName(filePath);
        String className = script.getFileName().replace(".java", "");

        if (packageName != null && !packageName.isEmpty()) {
            // 有包名：需要编译到对应目录结构
            String packageDir = packageName.replace('.', File.separatorChar);
            String sourceDir = Paths.get(filePath).getParent().toString();
            String outputDir = Paths.get(sourceDir, "build").toString();

            // 创建输出目录
            new File(outputDir).mkdirs();

            // 编译命令：javac -cp cms.jar -d build/ source.java
            // 运行命令：java -cp build;cms.jar package.ClassName
            return "javac -cp \"" + classpath + "\" -d \"" + outputDir + "\" \"" + filePath + "\" && java -cp \"" + outputDir + File.pathSeparator + classpath + "\" " + packageName + "." + className;
        } else {
            // 无包名：直接编译运行
            String outputDir = Paths.get(filePath).getParent().toString();
            return "javac -cp \"" + classpath + "\" \"" + filePath + "\" && java -cp \"" + outputDir + File.pathSeparator + classpath + "\" " + className;
        }
    }

    /**
     * 查找CMS类路径（优先使用编译后的classes目录+Maven依赖）
     */
    private String findCmsClasspath(String rootDirectory) {
        // 优先使用编译后的classes目录 + Maven依赖
        String classesDir = Paths.get(rootDirectory, "target", "classes").toString();
        if (Files.exists(Paths.get(classesDir))) {
            // 尝试获取Maven依赖classpath
            String mavenCp = getMavenClasspath(rootDirectory);
            if (mavenCp != null && !mavenCp.isEmpty()) {
                return mavenCp + File.pathSeparator + classesDir;
            }
            return classesDir;
        }
        // 其次尝试jar包
        String[] jarCandidates = {
            Paths.get(rootDirectory, "target", "cases-manage-system-1.0.0.jar").toString(),
            Paths.get(rootDirectory, "cases-manage-system-1.0.0.jar").toString()
        };
        for (String path : jarCandidates) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }
        return ".";
    }

    /**
     * 通过Maven获取依赖classpath
     */
    private String getMavenClasspath(String rootDirectory) {
        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "-q", "dependency:build-classpath",
                    "-Dmdep.outputFile=" + Paths.get(rootDirectory, "target", "cp.txt").toString());
            pb.directory(new File(rootDirectory));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);

            Path cpFile = Paths.get(rootDirectory, "target", "cp.txt");
            if (Files.exists(cpFile)) {
                String cp = Files.readString(cpFile).trim();
                Files.delete(cpFile);
                return cp;
            }
        } catch (Exception e) {
            // 忽略，回退到classes目录
        }
        return null;
    }

    /**
     * 从Java源文件中提取包名
     */
    private String extractPackageName(String filePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("package ")) {
                    int semiIndex = trimmed.indexOf(';');
                    if (semiIndex > 0) {
                        return trimmed.substring(8, semiIndex).trim();
                    }
                }
                // 遇到第一个非package/import的非空行就停止
                if (!trimmed.isEmpty() && !trimmed.startsWith("//") && !trimmed.startsWith("/*")
                        && !trimmed.startsWith("import ") && !trimmed.startsWith("package ")) {
                    break;
                }
            }
        } catch (IOException e) {
            // 忽略
        }
        return null;
    }

    /**
     * 查找关联的用例JSON（模糊匹配）
     */
    private String findAssociatedCase(TestScript script, String rootDirectory) {
        String scriptKey = normalizeName(script.getFileName());
        List<TestCase> cases = caseDao.scanCases(rootDirectory);
        for (TestCase tc : cases) {
            if (tc.getCaseInfo() != null && tc.getCaseInfo().getCaseName() != null) {
                String caseKey = normalizeName(tc.getCaseInfo().getCaseName());
                if (scriptKey.equals(caseKey)) {
                    return tc.getFilePath();
                }
            }
        }
        return null;
    }

    /**
     * 标准化名称
     */
    private String normalizeName(String name) {
        if (name == null) return "";
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        name = name.toLowerCase();
        name = name.replaceAll("(cases?|test|_cases?|_test)$", "");
        name = name.replaceAll("[_\\s]", "");
        return name;
    }

    /**
     * 截断输出
     */
    private String truncateOutput(String output) {
        if (output == null) return "";
        int maxLen = 2000;
        if (output.length() > maxLen) {
            return output.substring(0, maxLen) + "\n... (truncated)";
        }
        return output;
    }

    /**
     * 比较优先级（P0 > P1 > P2 > P3 > null）
     */
    private int comparePriority(String a, String b) {
        int pa = getPriorityValue(a);
        int pb = getPriorityValue(b);
        return Integer.compare(pa, pb);
    }

    /**
     * 获取优先级数值（越小优先级越高）
     */
    private int getPriorityValue(String priority) {
        if (priority == null || priority.isEmpty()) {
            return 999;
        }
        priority = priority.toUpperCase();
        if (priority.equals("P0")) return 0;
        if (priority.equals("P1")) return 1;
        if (priority.equals("P2")) return 2;
        if (priority.equals("P3")) return 3;
        return 999;
    }

    /**
     * 保存Debug日志
     */
    private void saveDebugLog(String scriptFileName, String output) {
        try {
            Files.createDirectories(Paths.get(debugLogDir));
            String baseName = scriptFileName.contains(".")
                    ? scriptFileName.substring(0, scriptFileName.lastIndexOf('.'))
                    : scriptFileName;
            String logFileName = baseName + "Debug.txt";
            Path logPath = Paths.get(debugLogDir, logFileName);
            Files.write(logPath, output.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Failed to save debug log: " + e.getMessage());
        }
    }

    /**
     * 清理过期Debug日志
     */
    private void cleanupDebugLogs() {
        try {
            Path logDir = Paths.get(debugLogDir);
            if (!Files.exists(logDir)) return;

            long cutoffTime = System.currentTimeMillis() - (Constants.DEBUG_LOG_RETENTION_DAYS * 24L * 60 * 60 * 1000);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir)) {
                for (Path entry : stream) {
                    if (Files.isRegularFile(entry)) {
                        long lastModified = Files.getLastModifiedTime(entry).toMillis();
                        if (lastModified < cutoffTime) {
                            Files.delete(entry);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to cleanup debug logs: " + e.getMessage());
        }
    }

    /**
     * 收集基线快照
     */
    private Map<String, String> collectBaseline() {
        Map<String, String> snapshot = new HashMap<>();

        // 操作系统
        snapshot.put("os.name", System.getProperty("os.name"));
        snapshot.put("os.version", System.getProperty("os.version"));
        snapshot.put("os.arch", System.getProperty("os.arch"));

        // Java版本
        snapshot.put("java.version", System.getProperty("java.version"));

        // Godot可执行文件MD5（如果存在）
        String godotPath = findGodotExecutable();
        if (godotPath != null) {
            snapshot.put("godot.path", godotPath);
            snapshot.put("godot.md5", getFileMD5(godotPath));
        }

        // 执行时间
        snapshot.put("execution.time", Instant.now().toString());

        return snapshot;
    }

    /**
     * 查找Godot可执行文件
     */
    private String findGodotExecutable() {
        String os = System.getProperty("os.name").toLowerCase();
        String[] searchPaths;

        if (os.contains("win")) {
            searchPaths = new String[]{
                    "C:\\Program Files\\Godot\\godot.exe",
                    "C:\\Program Files (x86)\\Godot\\godot.exe",
                    System.getenv("ProgramFiles") + "\\Godot\\godot.exe"
            };
        } else if (os.contains("mac")) {
            searchPaths = new String[]{
                    "/Applications/Godot.app/Contents/MacOS/Godot"
            };
        } else {
            searchPaths = new String[]{
                    "/usr/local/bin/godot",
                    "/usr/bin/godot"
            };
        }

        for (String path : searchPaths) {
            if (path != null && new File(path).exists()) {
                return path;
            }
        }
        return null;
    }

    /**
     * 计算文件MD5
     */
    private String getFileMD5(String filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            byte[] digest = md.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * 等待恢复
     */
    private void waitForResume() {
        synchronized (pauseLock) {
            while (paused.get()) {
                try {
                    pauseLock.wait(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 暂停执行
     */
    public void pause() {
        paused.set(true);
    }

    /**
     * 恢复执行
     */
    public void resume() {
        paused.set(false);
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }

    /**
     * 停止执行
     */
    public void stop() {
        running.set(false);
        paused.set(false);
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 是否暂停
     */
    public boolean isPaused() {
        return paused.get();
    }

    /**
     * 设置进度回调
     */
    public void setOnProgressChanged(Runnable handler) {
        this.onProgressChanged = handler;
    }

    /**
     * 更新关联用例状态
     */
    private void updateCaseStatuses(ReportData report) {
        for (ExecutionResult result : report.getResults()) {
            if (result.getTestCasePath() != null && !"NOT_EXECUTED".equals(result.getStatus())) {
                TestCase testCase = caseDao.loadCase(result.getTestCasePath());
                if (testCase != null) {
                    switch (result.getStatus()) {
                        case "PASSED":
                            testCase.setCurrentStatus(com.autocase.entity.CaseStatus.PASSED);
                            break;
                        case "FAILED":
                            testCase.setCurrentStatus(com.autocase.entity.CaseStatus.FAILED_NOT_REGRESSED);
                            break;
                        case "BLOCKED":
                            testCase.setCurrentStatus(com.autocase.entity.CaseStatus.BLOCKED);
                            break;
                        case "TIMEOUT":
                            testCase.setCurrentStatus(com.autocase.entity.CaseStatus.BLOCKED);
                            break;
                    }
                    caseDao.saveCase(testCase);
                }
            }
        }
    }
}

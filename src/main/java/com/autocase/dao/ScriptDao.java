package com.autocase.dao;

import com.autocase.entity.CaseInfo;
import com.autocase.entity.TestCase;
import com.autocase.entity.TestScript;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 测试脚本数据访问类 - 负责扫描和识别测试脚本
 */
public class ScriptDao {

    // 支持的语言及扩展名映射
    private static final Map<String, String> LANGUAGE_EXTENSIONS = new HashMap<>();

    static {
        LANGUAGE_EXTENSIONS.put("Python", ".py");
        LANGUAGE_EXTENSIONS.put("C", ".c");
        LANGUAGE_EXTENSIONS.put("C++", ".cpp");
        LANGUAGE_EXTENSIONS.put("C++", ".cc");
        LANGUAGE_EXTENSIONS.put("GDScript", ".gd");
        LANGUAGE_EXTENSIONS.put("C#", ".cs");
        LANGUAGE_EXTENSIONS.put("Java", ".java");
        LANGUAGE_EXTENSIONS.put("JavaScript", ".js");
        LANGUAGE_EXTENSIONS.put("TypeScript", ".ts");
        LANGUAGE_EXTENSIONS.put("Go", ".go");
        LANGUAGE_EXTENSIONS.put("Rust", ".rs");
        LANGUAGE_EXTENSIONS.put("Ruby", ".rb");
        LANGUAGE_EXTENSIONS.put("Shell", ".sh");
        LANGUAGE_EXTENSIONS.put("Batch", ".bat");
        LANGUAGE_EXTENSIONS.put("Batch", ".cmd");
    }

    /**
     * 扫描目录下的所有测试脚本
     * 规则：文件名包含 Test 或 _test 后缀
     */
    public List<TestScript> scanScripts(String directoryPath) {
        List<TestScript> scripts = new ArrayList<>();
        Path rootPath = Paths.get(directoryPath);

        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            return scripts;
        }

        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.filter(Files::isRegularFile)
                .filter(this::isTestScript)
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String language = detectLanguage(fileName);
                    if (language != null) {
                        scripts.add(new TestScript(path.toString(), fileName, language));
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return scripts;
    }

    /**
     * 判断是否为测试脚本（文件名包含 Test 或 _test 后缀）
     */
    private boolean isTestScript(Path path) {
        String fileName = path.getFileName().toString();
        // 去掉扩展名
        String nameWithoutExt = fileName;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = fileName.substring(0, dotIndex);
        }
        // 检查是否以 Test 或 _test 结尾
        return nameWithoutExt.endsWith("Test") || nameWithoutExt.endsWith("_test");
    }

    /**
     * 根据扩展名检测语言
     */
    private String detectLanguage(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return null;
        }
        String ext = fileName.substring(dotIndex).toLowerCase();
        for (Map.Entry<String, String> entry : LANGUAGE_EXTENSIONS.entrySet()) {
            if (entry.getValue().equals(ext)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 获取支持的语言列表
     */
    public List<String> getSupportedLanguages() {
        return new ArrayList<>(LANGUAGE_EXTENSIONS.keySet());
    }

    /**
     * 根据用例列表为脚本补充优先级和步骤数信息
     */
    public void enrichScriptsWithCaseInfo(List<TestScript> scripts, List<TestCase> cases) {
        Map<String, TestCase> caseMap = new HashMap<>();
        for (TestCase tc : cases) {
            caseMap.put(tc.getCaseName() != null ? tc.getCaseName().toLowerCase() : "", tc);
            caseMap.put(tc.getCasesID() != null ? tc.getCasesID().toLowerCase() : "", tc);
        }

        for (TestScript script : scripts) {
            String caseKey = script.getCaseName().toLowerCase();
            TestCase matchedCase = caseMap.get(caseKey);
            if (matchedCase == null) {
                // 尝试模糊匹配
                for (Map.Entry<String, TestCase> entry : caseMap.entrySet()) {
                    if (entry.getKey().contains(caseKey) || caseKey.contains(entry.getKey())) {
                        matchedCase = entry.getValue();
                        break;
                    }
                }
            }

            if (matchedCase != null) {
                if (matchedCase.getExecution() != null) {
                    script.setPriority(matchedCase.getExecution().getPriority());
                }
                if (matchedCase.getCaseInfo() != null && matchedCase.getCaseInfo().getStepsToReproduce() != null) {
                    script.setStepsCount(matchedCase.getCaseInfo().getStepsToReproduce().size());
                }
            }
        }
    }
}

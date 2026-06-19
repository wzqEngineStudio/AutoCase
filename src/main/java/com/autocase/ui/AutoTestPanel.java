package com.autocase.ui;

import com.autocase.dao.CaseDao;
import com.autocase.dao.ConfigDao;
import com.autocase.dao.ExecutionHistoryDao;
import com.autocase.dao.ReportDao;
import com.autocase.dao.ScriptDao;
import com.autocase.entity.*;
import com.autocase.logic.ScriptExecutor;
import com.autocase.util.DialogUtil;
import com.autocase.util.LayoutPersistence;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动化测试面板
 */
public class AutoTestPanel extends BorderPane {

    private final ScriptExecutor scriptExecutor;
    private final CaseDao caseDao;
    private final ScriptDao scriptDao;
    private final ReportDao reportDao;
    private final ConfigDao configDao;
    private ConfigDao.CmsConfig config;

    private String rootDirectory;
    private String lastViewedScriptPath;
    private List<TestScript> scripts;

    // UI组件
    private ScriptExplorer scriptExplorer;
    private Button scanBtn;
    private Button clearScriptsBtn;
    private Button runAllBtn;
    private Button runSelectedBtn;
    private Button pauseBtn;
    private Button stopBtn;
    private Button settingsBtn;

    private ProgressBar progressBar;
    private Label progressLabel;

    private TableView<TestScript> scriptTable;
    private TableColumn<TestScript, Boolean> selectCol;
    private TableColumn<TestScript, String> nameCol;
    private TableColumn<TestScript, String> languageCol;
    private TableColumn<TestScript, String> caseNameCol;

    // 报告回调
    private java.util.function.Consumer<ReportData> onReportGenerated;

    // 执行历史 DAO
    private final ExecutionHistoryDao executionHistoryDao;

    public AutoTestPanel() {
        this.scriptExecutor = new ScriptExecutor();
        this.caseDao = new CaseDao();
        this.scriptDao = new ScriptDao();
        this.reportDao = new ReportDao();
        this.configDao = new ConfigDao();
        this.executionHistoryDao = new ExecutionHistoryDao();
        this.config = configDao.loadConfig();
        buildUI();
    }

    /**
     * 设置根目录（与用例管理共用）
     */
    public void setRootDirectory(String directory) {
        if (directory == null) return;
        this.rootDirectory = directory;
        scriptExplorer.setRootDirectory(directory);
        updateButtonStates();
        
        // 重新加载配置以获取最新状态
        this.config = configDao.loadConfig();
        
        // 尝试恢复上次扫描的脚本列表
        restoreScannedScripts();
        
        // 延迟恢复上次查看的脚本（等待树构建完成）
        if (lastViewedScriptPath != null) {
            Platform.runLater(() -> {
                scriptExplorer.expandToPath(lastViewedScriptPath);
                selectScriptByPath(lastViewedScriptPath);
            });
        }
    }

    /**
     * 恢复上次扫描的脚本列表
     */
    private void restoreScannedScripts() {
        List<String> savedPaths = config.getLastScannedScripts();
        if (savedPaths == null || savedPaths.isEmpty()) return;
        
        // 验证文件是否仍然存在
        List<TestScript> restoredScripts = new ArrayList<>();
        for (String path : savedPaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                String fileName = file.getName();
                String language = detectLanguage(fileName);
                TestScript script = new TestScript(path, fileName, language);
                script.setSelected(true);
                restoredScripts.add(script);
            }
        }
        
        if (!restoredScripts.isEmpty()) {
            this.scripts = restoredScripts;
            scriptTable.getItems().setAll(restoredScripts);
            progressLabel.setText("已恢复 " + restoredScripts.size() + " 个脚本（上次扫描）");
        }
    }

    /**
     * 检测脚本语言
     */
    private String detectLanguage(String fileName) {
        if (fileName == null) return "unknown";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".py")) return "Python";
        if (lower.endsWith(".java")) return "Java";
        if (lower.endsWith(".js")) return "JavaScript";
        if (lower.endsWith(".ts")) return "TypeScript";
        if (lower.endsWith(".gd")) return "GDScript";
        if (lower.endsWith(".cs")) return "C#";
        return "unknown";
    }

    private void buildUI() {
        // 顶部工具栏
        HBox toolBar = createToolBar();

        // 中间：左侧脚本资源管理器 + 右侧脚本列表
        scriptExplorer = new ScriptExplorer();
        scriptExplorer.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            String filePath = scriptExplorer.getSelectedFilePath();
            if (filePath != null && new File(filePath).isFile()) {
                selectScriptByPath(filePath);
            }
        });

        scriptTable = createScriptTable();
        ScrollPane scrollPane = new ScrollPane(scriptTable);
        scrollPane.setFitToWidth(true);

        SplitPane centerSplit = new SplitPane();
        centerSplit.getItems().addAll(scriptExplorer, scrollPane);
        double[] autoLayout = LayoutPersistence.getPositions("AutoTestPanel", new double[]{0.3});
        centerSplit.setDividerPositions(autoLayout);
        centerSplit.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            LayoutPersistence.savePositions("AutoTestPanel", centerSplit.getDividerPositions());
        });

        // 底部：进度条
        VBox bottomBox = createBottomBar();

        setTop(toolBar);
        setCenter(centerSplit);
        setBottom(bottomBox);
    }

    /**
     * 根据文件路径自动勾选对应脚本
     */
    private void selectScriptByPath(String filePath) {
        if (scripts == null) return;
        for (TestScript script : scripts) {
            if (script.getFilePath().equals(filePath)) {
                script.setSelected(!script.isSelected());
                scriptTable.refresh();
                lastViewedScriptPath = filePath;
                break;
            }
        }
    }

    private HBox createToolBar() {
        HBox toolBar = new HBox(10);
        toolBar.setPadding(new Insets(8, 10, 8, 10));
        toolBar.setStyle("-fx-background-color: #f0f0f0;");
        toolBar.setAlignment(Pos.CENTER_LEFT);

        scanBtn = new Button("扫描脚本");
        clearScriptsBtn = new Button("清除脚本列表");
        runAllBtn = new Button("执行全部");
        runSelectedBtn = new Button("执行选中");
        pauseBtn = new Button("暂停");
        stopBtn = new Button("停止");
        settingsBtn = new Button("执行设置");

        scanBtn.setOnAction(e -> scanScripts());
        clearScriptsBtn.setOnAction(e -> clearScriptsList());
        runAllBtn.setOnAction(e -> runAllScripts());
        runSelectedBtn.setOnAction(e -> runSelectedScripts());
        pauseBtn.setOnAction(e -> togglePause());
        stopBtn.setOnAction(e -> stopExecution());
        settingsBtn.setOnAction(e -> showSettingsDialog());

        updateButtonStates();

        toolBar.getChildren().addAll(scanBtn, clearScriptsBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                runAllBtn, runSelectedBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                pauseBtn, stopBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                settingsBtn);

        return toolBar;
    }

    private TableView<TestScript> createScriptTable() {
        TableView<TestScript> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        selectCol = new TableColumn<>("选择");
        selectCol.setPrefWidth(60);
        selectCol.setCellFactory(col -> new TableCell<TestScript, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(e -> {
                    TestScript script = getTableView().getItems().get(getIndex());
                    script.setSelected(checkBox.isSelected());
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setGraphic(null);
                } else {
                    TestScript script = getTableView().getItems().get(getIndex());
                    checkBox.setSelected(script.isSelected());
                    setGraphic(checkBox);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        nameCol = new TableColumn<>("脚本名称");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFileName()));

        languageCol = new TableColumn<>("语言");
        languageCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getLanguage()));
        languageCol.setPrefWidth(80);

        caseNameCol = new TableColumn<>("关联用例");
        caseNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCaseName()));

        table.getColumns().addAll(selectCol, nameCol, languageCol, caseNameCol);
        return table;
    }

    private VBox createBottomBar() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(5, 10, 5, 10));
        box.setStyle("-fx-background-color: #f5f5f5;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressLabel = new Label("就绪");
        progressLabel.setStyle("-fx-font-size: 12px;");

        HBox progressBox = new HBox(10, progressBar, progressLabel);
        progressBox.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().add(progressBox);
        return box;
    }

    private void scanScripts() {
        if (rootDirectory == null) {
            DialogUtil.showError("请先在用例管理面板选择根目录");
            return;
        }

        scripts = scriptExecutor.scanScripts(rootDirectory);
        scriptTable.getItems().setAll(scripts);
        progressLabel.setText("发现 " + scripts.size() + " 个测试脚本");
        
        // 保存扫描结果
        saveScannedScripts();
        
        DialogUtil.showInfoWithTitle("扫描完成", "共发现 " + scripts.size() + " 个测试脚本");
    }

    /**
     * 保存扫描的脚本列表到配置
     */
    private void saveScannedScripts() {
        if (scripts == null) return;
        // 先重新加载配置，避免覆盖其他模块保存的数据
        this.config = configDao.loadConfig();
        List<String> paths = new ArrayList<>();
        for (TestScript script : scripts) {
            paths.add(script.getFilePath());
        }
        config.setLastScannedScripts(paths);
        configDao.saveConfig(config);
    }

    /**
     * 清除脚本列表
     */
    private void clearScriptsList() {
        if (scripts == null || scripts.isEmpty()) {
            DialogUtil.showError("脚本列表已为空");
            return;
        }
        // 先重新加载配置
        this.config = configDao.loadConfig();
        scripts.clear();
        scriptTable.getItems().clear();
        config.setLastScannedScripts(new ArrayList<>());
        configDao.saveConfig(config);
        progressLabel.setText("脚本列表已清除");
    }

    private void runAllScripts() {
        if (scripts == null || scripts.isEmpty()) {
            DialogUtil.showError("没有可执行的脚本");
            return;
        }
        scripts.forEach(s -> s.setSelected(true));
        scriptTable.refresh();
        executeScripts();
    }

    private void runSelectedScripts() {
        if (scripts == null || scripts.isEmpty()) {
            DialogUtil.showError("没有可执行的脚本");
            return;
        }
        long selectedCount = scripts.stream().filter(TestScript::isSelected).count();
        if (selectedCount == 0) {
            DialogUtil.showError("请至少选择一个脚本");
            return;
        }
        executeScripts();
    }

    private void executeScripts() {
        updateButtonStates();
        progressLabel.setText("正在执行...");

        scriptExecutor.setOnProgressChanged(() -> {
            int completed = (int) scriptTable.getItems().stream()
                    .filter(s -> !"NOT_EXECUTED".equals(getResultStatus(s)))
                    .count();
            int total = scripts.size();
            double progress = total > 0 ? (double) completed / total : 0;
            Platform.runLater(() -> {
                progressBar.setProgress(progress);
                progressLabel.setText(String.format("执行中: %d/%d", completed, total));
            });
        });

        new Thread(() -> {
            // 为脚本补充用例信息（优先级、步骤数）
            List<TestCase> allCases = caseDao.scanCases(rootDirectory);
            scriptDao.enrichScriptsWithCaseInfo(scripts, allCases);

            ReportData report = scriptExecutor.executeScripts(rootDirectory, scripts);

            // 写入 H2 执行历史
            saveToExecutionHistory(report);

            Platform.runLater(() -> {
                progressBar.setProgress(1.0);
                progressLabel.setText(String.format("执行完成 - 通过: %d, 失败: %d, 阻塞: %d, 超时: %d, 跳过: %d, 未执行: %d, 通过率: %.1f%%",
                        report.getPassedScripts(), report.getFailedScripts(),
                        report.getBlockedScripts(), report.getTimeoutScripts(),
                        report.getSkippedScripts(), report.getNotExecutedScripts(),
                        report.getPassRate()));

                reportDao.saveReport(report);

                if (onReportGenerated != null) {
                    onReportGenerated.accept(report);
                }

                updateButtonStates();
            });
        }).start();
    }

    /**
     * 将执行结果写入 H2 数据库
     */
    private void saveToExecutionHistory(ReportData report) {
        ExecutionBatch batch = new ExecutionBatch();
        batch.setTriggerType("MANUAL");
        batch.setTriggerUser("CMS-User");
        batch.setTotalCases(report.getTotalScripts());
        batch.setPassedCount(report.getPassedScripts());
        batch.setFailedCount(report.getFailedScripts());
        batch.setBlockedCount(report.getBlockedScripts());
        batch.setStartTime(new Timestamp(report.getStartTime()));
        batch.setEndTime(new Timestamp(report.getEndTime()));
        batch.setGitCommit(null);

        Long batchId = executionHistoryDao.insertBatch(batch);
        if (batchId == null) return;

        for (ExecutionResult result : report.getResults()) {
            ExecutionDetail detail = new ExecutionDetail();
            detail.setBatchId(batchId);
            detail.setCaseId(result.getCaseName());
            detail.setStatus(result.getStatus());
            detail.setDurationMs((int) result.getExecutionTime());

            // 失败原因
            if ("FAILED".equals(result.getStatus())) {
                detail.setFailureReason(result.getErrorMessage());
            }

            // 输出日志
            detail.setOutputLog(result.getActualOutput());

            // 期望结果和实际结果
            detail.setExpectedResult(result.getExpectedOutput());
            detail.setActualResult(result.getActualOutput());

            detail.setStackTrace(null);
            detail.setExecutionTime(new Timestamp(System.currentTimeMillis()));

            executionHistoryDao.insertDetail(detail);
        }
    }

    private String getResultStatus(TestScript script) {
        return "NOT_EXECUTED";
    }

    private void togglePause() {
        if (scriptExecutor.isPaused()) {
            scriptExecutor.resume();
            pauseBtn.setText("暂停");
            progressLabel.setText("继续执行...");
        } else {
            scriptExecutor.pause();
            pauseBtn.setText("继续");
            progressLabel.setText("已暂停");
        }
    }

    private void stopExecution() {
        scriptExecutor.stop();
        progressBar.setProgress(0);
        progressLabel.setText("已停止");
        pauseBtn.setText("暂停");
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean running = scriptExecutor.isRunning();
        runAllBtn.setDisable(running || rootDirectory == null);
        runSelectedBtn.setDisable(running || rootDirectory == null);
        scanBtn.setDisable(running);
        pauseBtn.setDisable(!running);
        stopBtn.setDisable(!running);
    }

    private void showSettingsDialog() {
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("执行设置");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(100);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        col1.setMinWidth(250);
        grid.getColumnConstraints().addAll(col0, col1);

        // 判定规则
        TextField passField = new TextField("PASS");
        TextField failField = new TextField("FAIL");
        TextField blockField = new TextField("BLOCK");

        // 超时设置
        Spinner<Integer> timeoutSpinner = new Spinner<>(1, 99999, config.getExecutionTimeout() / 1000);
        timeoutSpinner.setEditable(true);

        grid.add(new Label("通过关键字:"), 0, 0);
        grid.add(passField, 1, 0);
        grid.add(new Label("失败关键字:"), 0, 1);
        grid.add(failField, 1, 1);
        grid.add(new Label("阻塞关键字:"), 0, 2);
        grid.add(blockField, 1, 2);
        grid.add(new Label("超时时间(秒):"), 0, 3);
        grid.add(timeoutSpinner, 1, 3);

        // 自定义命令
        Label hintLabel = new Label("自定义执行命令（可选，留空使用默认）:");
        GridPane.setConstraints(hintLabel, 0, 4, 2, 1);
        grid.getChildren().add(hintLabel);

        TextArea customCmdArea = new TextArea();
        customCmdArea.setPromptText("Python=python \"{0}\"\nJava=java -cp \"{1}\" \"{2}\"");
        customCmdArea.setPrefRowCount(4);
        GridPane.setConstraints(customCmdArea, 0, 5, 2, 1);
        grid.getChildren().add(customCmdArea);

        Button saveBtn = new Button("保存");
        Button cancelBtn = new Button("取消");
        HBox btnBox = new HBox(10, saveBtn, cancelBtn);
        GridPane.setConstraints(btnBox, 1, 6);
        grid.getChildren().add(btnBox);

        saveBtn.setOnAction(e -> {
            scriptExecutor.setPatterns(passField.getText(), failField.getText(), blockField.getText());
            scriptExecutor.setExecutionTimeout(timeoutSpinner.getValue() * 1000);

            // 保存超时配置
            config.setExecutionTimeout(timeoutSpinner.getValue() * 1000);
            configDao.saveConfig(config);

            // 解析自定义命令
            String customText = customCmdArea.getText();
            if (customText != null && !customText.trim().isEmpty()) {
                java.util.Map<String, String> customCmds = new java.util.HashMap<>();
                for (String line : customText.split("\n")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        customCmds.put(parts[0].trim(), parts[1].trim());
                    }
                }
                scriptExecutor.setCustomCommands(customCmds);
            }

            dialog.close();
        });

        cancelBtn.setOnAction(e -> dialog.close());

        Scene scene = new Scene(grid, 450, 350);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public void setOnReportGenerated(java.util.function.Consumer<ReportData> handler) {
        this.onReportGenerated = handler;
    }

    public String getLastViewedScriptPath() {
        return lastViewedScriptPath;
    }
}

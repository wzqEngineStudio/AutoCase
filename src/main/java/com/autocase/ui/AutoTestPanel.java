package com.autocase.ui;

import com.autocase.dao.CaseDao;
import com.autocase.dao.ConfigDao;
import com.autocase.dao.ExecutionHistoryDao;
import com.autocase.dao.ManualTestDao;
import com.autocase.dao.ReportDao;
import com.autocase.dao.ScriptDao;
import com.autocase.entity.*;
import com.autocase.logic.ScriptExecutor;
import com.autocase.logic.ScriptTemplateGenerator;
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
import java.util.Map;

/**
 * 自动化测试面板
 */
public class AutoTestPanel extends BorderPane {

    private final ScriptExecutor scriptExecutor;
    private final CaseDao caseDao;
    private final ScriptDao scriptDao;
    private final ReportDao reportDao;
    private final ConfigDao configDao;
    private final ManualTestDao manualTestDao;
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
        this.manualTestDao = new ManualTestDao();
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
        Button templateBtn = new Button("生成脚本模板");

        scanBtn.setOnAction(e -> scanScripts());
        clearScriptsBtn.setOnAction(e -> clearScriptsList());
        runAllBtn.setOnAction(e -> runAllScripts());
        runSelectedBtn.setOnAction(e -> runSelectedScripts());
        pauseBtn.setOnAction(e -> togglePause());
        stopBtn.setOnAction(e -> stopExecution());
        settingsBtn.setOnAction(e -> showSettingsDialog());
        templateBtn.setOnAction(e -> showTemplateDialog());

        updateButtonStates();

        toolBar.getChildren().addAll(scanBtn, clearScriptsBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                runAllBtn, runSelectedBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                pauseBtn, stopBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                settingsBtn, templateBtn);

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

    /**
     * 显示脚本模板生成对话框
     * 根据选中的用例生成指定语言的测试脚本模板
     */
    private void showTemplateDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("生成脚本模板");
        dialog.setWidth(820);
        dialog.setHeight(660);

        // ===== 筛选栏（复用 CaseFilterPanel 的全部维度 + 新增分组） =====
        HBox filterBox = new HBox(8);
        filterBox.setPadding(new Insets(6));
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label("筛选:");
        ComboBox<String> filterTypeCombo = new ComboBox<>();
        filterTypeCombo.getItems().addAll("关键字", "编号范围", "状态", "严重程度", "优先级", "分组");
        filterTypeCombo.setValue("关键字");

        // 筛选值控件（动态切换）
        TextField keywordField = new TextField();
        keywordField.setPromptText("输入关键字");

        TextField idStartField = new TextField();
        idStartField.setPromptText("起始");
        idStartField.setPrefWidth(70);
        TextField idEndField = new TextField();
        idEndField.setPromptText("结束");
        idEndField.setPrefWidth(70);

        ComboBox<CaseStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(CaseStatus.values());
        statusCombo.setPromptText("选择状态");

        ComboBox<String> severityCombo = new ComboBox<>();
        severityCombo.setPromptText("严重程度");

        ComboBox<String> priorityCombo = new ComboBox<>();
        priorityCombo.setPromptText("优先级");

        ComboBox<String> groupCombo = new ComboBox<>();
        groupCombo.setPromptText("选择分组");

        // 筛选值容器
        HBox valueBox = new HBox(5);
        valueBox.getChildren().addAll(keywordField, idStartField, idEndField,
                statusCombo, severityCombo, priorityCombo, groupCombo);
        HBox.setHgrow(valueBox, Priority.ALWAYS);

        Button filterBtn = new Button("筛选");
        Button resetBtn = new Button("重置");

        filterBox.getChildren().addAll(typeLabel, filterTypeCombo, valueBox, filterBtn, resetBtn);

        // 筛选类型切换 → 控制控件显隐
        Runnable updateFilterControls = () -> {
            String type = filterTypeCombo.getValue();
            boolean showKeyword = "关键字".equals(type);
            boolean showIdRange = "编号范围".equals(type);
            boolean showStatus = "状态".equals(type);
            boolean showSeverity = "严重程度".equals(type);
            boolean showPriority = "优先级".equals(type);
            boolean showGroup = "分组".equals(type);

            keywordField.setVisible(showKeyword);
            keywordField.setManaged(showKeyword);
            idStartField.setVisible(showIdRange);
            idStartField.setManaged(showIdRange);
            idEndField.setVisible(showIdRange);
            idEndField.setManaged(showIdRange);
            statusCombo.setVisible(showStatus);
            statusCombo.setManaged(showStatus);
            severityCombo.setVisible(showSeverity);
            severityCombo.setManaged(showSeverity);
            priorityCombo.setVisible(showPriority);
            priorityCombo.setManaged(showPriority);
            groupCombo.setVisible(showGroup);
            groupCombo.setManaged(showGroup);
        };
        filterTypeCombo.setOnAction(e -> updateFilterControls.run());
        updateFilterControls.run();

        // ---- 用例选择行 ----
        HBox caseRow = new HBox(10);
        caseRow.setAlignment(Pos.CENTER_LEFT);
        caseRow.setPadding(new Insets(0, 0, 6, 0));
        Label caseLabel = new Label("选择用例:");
        ComboBox<String> caseCombo = new ComboBox<>();
        caseCombo.setPrefWidth(320);

        caseRow.getChildren().addAll(caseLabel, caseCombo);

        // ---- 语言 + 生成到目录 行 ----
        HBox optionRow = new HBox(10);
        optionRow.setAlignment(Pos.CENTER_LEFT);
        optionRow.setPadding(new Insets(0, 0, 6, 0));

        Label langLabel = new Label("目标语言:");
        ComboBox<String> langCombo = new ComboBox<>();
        for (Map.Entry<String, String> entry : ScriptTemplateGenerator.SUPPORTED_LANGUAGES.entrySet()) {
            langCombo.getItems().add(entry.getKey());
        }
        if (!langCombo.getItems().isEmpty()) {
            langCombo.setValue(langCombo.getItems().get(0));
        }

        Label targetDirLabel = new Label("生成到:");
        TextField targetDirField = new TextField();
        targetDirField.setPrefWidth(240);
        targetDirField.setEditable(false);
        targetDirField.setStyle("-fx-text-fill: #333;");
        String defaultTargetDir = resolveTargetDirectory();
        targetDirField.setText(defaultTargetDir != null ? defaultTargetDir :
                (rootDirectory != null ? rootDirectory : "."));
        Button browseDirBtn = new Button("浏览...");
        browseDirBtn.setOnAction(e -> {
            javafx.stage.DirectoryChooser dirChooser = new javafx.stage.DirectoryChooser();
            dirChooser.setTitle("选择脚本输出目录");
            String currentText = targetDirField.getText();
            if (currentText != null && !currentText.isEmpty()) {
                File currentDir = new File(currentText);
                if (currentDir.exists() && currentDir.isDirectory()) {
                    dirChooser.setInitialDirectory(currentDir);
                }
            }
            File chosen = dirChooser.showDialog(dialog);
            if (chosen != null) {
                targetDirField.setText(chosen.getAbsolutePath());
            }
        });

        optionRow.getChildren().addAll(langLabel, langCombo,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                targetDirLabel, targetDirField, browseDirBtn);

        // 模板预览区域（只读）
        TextArea templateArea = new TextArea();
        templateArea.setEditable(false);
        templateArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px;");
        templateArea.setWrapText(true);

        // 操作按钮
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10));
        Button copyBtn = new Button("复制到剪贴板");
        copyBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        Button generateToBtn = new Button("生成到目录");
        generateToBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        Button closeBtn = new Button("关闭");
        btnBox.getChildren().addAll(copyBtn, generateToBtn, closeBtn);

        // 布局
        VBox topBar = new VBox(4);
        topBar.setPadding(new Insets(5));
        topBar.getChildren().addAll(filterBox, caseRow, optionRow);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(templateArea);
        root.setBottom(btnBox);

        // ===== 数据加载与筛选逻辑 =====

        /** 不关联用例时的固定选项值（必须在 lambda 之前声明，否则编译报错"找不到符号"） */
        final String NO_CASE_OPTION = "(不关联用例)";

        /** 全部用例缓存 */
        @SuppressWarnings("unchecked")
        final List<TestCase>[] allCasesHolder = new List[]{null};

        /** 加载全部用例并填充下拉选项 */
        java.util.function.Consumer<Void> loadAllCases = v -> {
            List<TestCase> cases = rootDirectory != null ? caseDao.scanCases(rootDirectory) : new ArrayList<>();
            allCasesHolder[0] = cases;

            // 填充分组下拉选项：从手工执行的任务组获取（而非 TestCase.group 字段）
            List<com.autocase.entity.TaskGroup> taskGroups = manualTestDao.getAllTaskGroups();
            groupCombo.getItems().clear();
            for (com.autocase.entity.TaskGroup tg : taskGroups) {
                groupCombo.getItems().add(tg.getGroupName());
            }

            // 填充严重程度下拉选项
            java.util.Set<String> severities = new java.util.LinkedHashSet<>();
            for (TestCase tc : cases) {
                String s = tc.getSeverity();
                if (s != null && !s.isEmpty()) {
                    severities.add(s);
                }
            }
            severityCombo.getItems().clear();
            severityCombo.getItems().addAll(severities);

            // 填充优先级下拉选项
            java.util.Set<String> priorities = new java.util.LinkedHashSet<>();
            for (TestCase tc : cases) {
                String p = tc.getPriority();
                if (p != null && !p.isEmpty()) {
                    priorities.add(p);
                }
            }
            priorityCombo.getItems().clear();
            priorityCombo.getItems().addAll(priorities);

            // 自动执行一次筛选
            applyTemplateFilter(filterTypeCombo.getValue(), keywordField, idStartField, idEndField,
                    statusCombo, severityCombo, priorityCombo, groupCombo, caseCombo, cases,
                    NO_CASE_OPTION);
        };

        // 初始化加载
        loadAllCases.accept(null);

        // 筛选按钮事件
        filterBtn.setOnAction(e ->
                applyTemplateFilter(filterTypeCombo.getValue(), keywordField, idStartField, idEndField,
                        statusCombo, severityCombo, priorityCombo, groupCombo, caseCombo,
                        allCasesHolder[0] != null ? allCasesHolder[0] : new ArrayList<>(),
                        NO_CASE_OPTION));

        // 重置按钮
        resetBtn.setOnAction(e -> {
            keywordField.clear();
            idStartField.clear();
            idEndField.clear();
            statusCombo.setValue(null);
            severityCombo.setValue(null);
            priorityCombo.setValue(null);
            groupCombo.setValue(null);
            filterTypeCombo.setValue("关键字");
            applyTemplateFilter("关键字", keywordField, idStartField, idEndField,
                    statusCombo, severityCombo, priorityCombo, groupCombo, caseCombo,
                    allCasesHolder[0] != null ? allCasesHolder[0] : new ArrayList<>(),
                    NO_CASE_OPTION);
        });

        // 切换用例或语言时重新生成模板
        Runnable generateTemplate = () -> {
            String selected = caseCombo.getValue();
            String language = langCombo.getValue();
            if (selected == null || language == null) return;

            try {
                String template;
                if (NO_CASE_OPTION.equals(selected)) {
                    // 不关联用例 → 生成空壳模板（CASE_NAME 为空串）
                    template = ScriptTemplateGenerator.generateStandalone(language);
                } else {
                    // 绑定用例 → 正常生成
                    String caseId = selected.split(" - ")[0];
                    TestCase targetCase = findCaseById(caseId);
                    if (targetCase == null) {
                        templateArea.setText("// 未找到用例: " + caseId);
                        return;
                    }
                    template = ScriptTemplateGenerator.generate(targetCase, language);
                }
                templateArea.setText(template);
            } catch (Exception ex) {
                templateArea.setText("// 生成失败: " + ex.getMessage());
            }
        };

        caseCombo.setOnAction(e -> generateTemplate.run());
        langCombo.setOnAction(e -> generateTemplate.run());

        // 复制功能
        copyBtn.setOnAction(e -> {
            String text = templateArea.getText();
            if (text != null && !text.isEmpty()) {
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(text);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                DialogUtil.showInfo("已复制到剪贴板");
            }
        });

        // 生成到目录功能
        generateToBtn.setOnAction(e -> {
            String selected = caseCombo.getValue();
            String language = langCombo.getValue();
            String targetDir = targetDirField.getText();
            if (selected == null || language == null) {
                DialogUtil.showError("请先选择用例和目标语言");
                return;
            }
            if (targetDir == null || targetDir.trim().isEmpty()) {
                DialogUtil.showError("请先选择生成目录");
                return;
            }

            String ext = getExtension(language);
            File outputDir = new File(targetDir.trim());

            if (NO_CASE_OPTION.equals(selected)) {
                // ===== 不关联用例：检查 CASE_NAME 决定命名方式 =====
                String templateText = templateArea.getText();
                // 从模板中提取 CASE_NAME 的值
                String caseNameFromTemplate = extractCaseNameFromTemplate(templateText, language);

                if (caseNameFromTemplate != null && !caseNameFromTemplate.isEmpty()) {
                    // 用户在模板中填写了 CASE_NAME → 直接用它命名
                    String fileName = sanitizeFileName(caseNameFromTemplate) + "_Test" + ext;
                    File outputFile = new File(outputDir, fileName);
                    writeOutputFile(outputFile, templateText);
                } else {
                    // CASE_NAME 为空 → 弹窗询问文件名
                    DialogUtil.showInputDialog("保存脚本模板", "请输入文件名:", "untitled" + ext, fileName -> {
                        if (fileName == null || fileName.trim().isEmpty()) return;
                        File outputFile = new File(outputDir, fileName.trim());
                        writeOutputFile(outputFile, templateText);
                    });
                }
            } else {
                // ===== 绑定用例：自动命名（用例名_Test.ext） ======
                String caseId = selected.split(" - ")[0];
                TestCase targetCase = findCaseById(caseId);
                if (targetCase == null) {
                    DialogUtil.showError("未找到用例: " + caseId);
                    return;
                }
                String defaultName = sanitizeFileName(targetCase.getCaseName()) + "_Test" + ext;
                File outputFile = new File(outputDir, defaultName);
                writeOutputFile(outputFile, templateArea.getText());
            }
        });

        closeBtn.setOnAction(e -> dialog.close());

        // 首次自动生成
        generateTemplate.run();

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.show();
    }

    /** 安全解析整数 */
    private static Integer parseIntSafe(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try { return Integer.parseInt(text.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    /**
     * 解析默认目标目录：优先使用资源管理器当前选中目录，回退到根目录
     */
    private String resolveTargetDirectory() {
        // 尝试从资源管理器获取当前选中路径（如果是目录）
        String scriptPath = scriptExplorer.getSelectedFilePath();
        if (scriptPath != null) {
            File f = new File(scriptPath);
            if (f.isFile()) {
                return f.getParent();
            }
            return scriptPath;
        }
        return rootDirectory;
    }

    /**
     * 根据当前筛选条件过滤用例并更新下拉框
     */
    private void applyTemplateFilter(String filterType, TextField kwField, TextField idStart, TextField idEnd,
            ComboBox<CaseStatus> stCombo, ComboBox<String> sevCombo, ComboBox<String> priCombo,
            ComboBox<String> grpCombo, ComboBox<String> caseCb, List<TestCase> allCases,
            String noCaseOption) {

        List<TestCase> filtered = allCases;

        if ("关键字".equals(filterType)) {
            String kw = kwField.getText().trim();
            if (!kw.isEmpty()) {
                final String keyword = kw.toLowerCase();
                filtered = new ArrayList<>();
                for (TestCase tc : allCases) {
                    String name = tc.getCaseInfo() != null ? tc.getCaseInfo().getCaseName() : "";
                    String id = tc.getCasesID() != null ? tc.getCasesID() : "";
                    if (name.toLowerCase().contains(keyword) || id.toLowerCase().contains(keyword)) {
                        filtered.add(tc);
                    }
                }
            }
        } else if ("编号范围".equals(filterType)) {
            Integer start = parseIntSafe(idStart.getText());
            Integer end = parseIntSafe(idEnd.getText());
            if (start != null && end != null) {
                final int s = start, e = end;
                filtered = new ArrayList<>();
                for (TestCase tc : allCases) {
                    String id = tc.getCasesID();
                    try {
                        int num = Integer.parseInt(id.replaceAll("\\D", ""));
                        if (num >= s && num <= e) filtered.add(tc);
                    } catch (NumberFormatException ignored) {}
                }
            }
        } else if ("状态".equals(filterType)) {
            CaseStatus st = stCombo.getValue();
            if (st != null) {
                final CaseStatus status = st;
                filtered = new ArrayList<>();
                for (TestCase tc : allCases) {
                    if (status.equals(tc.getCurrentStatus())) filtered.add(tc);
                }
            }
        } else if ("严重程度".equals(filterType)) {
            String sev = sevCombo.getValue();
            if (sev != null && !sev.isEmpty()) {
                final String severity = sev;
                filtered = new ArrayList<>();
                for (TestCase tc : allCases) {
                    if (severity.equals(tc.getSeverity())) filtered.add(tc);
                }
            }
        } else if ("优先级".equals(filterType)) {
            String pri = priCombo.getValue();
            if (pri != null && !pri.isEmpty()) {
                final String priority = pri;
                filtered = new ArrayList<>();
                for (TestCase tc : allCases) {
                    if (priority.equals(tc.getPriority())) filtered.add(tc);
                }
            }
        } else if ("分组".equals(filterType)) {
            String grp = grpCombo.getValue();
            if (grp != null && !grp.isEmpty()) {
                // 通过任务组名称查找该组下所有用例ID
                com.autocase.entity.TaskGroup targetGroup = manualTestDao.getTaskGroupByName(grp);
                java.util.Set<String> groupCaseIds = new java.util.LinkedHashSet<>();
                if (targetGroup != null) {
                    List<com.autocase.entity.ManualTestBatch> batches = manualTestDao.getBatchesByGroupId(targetGroup.getGroupId());
                    for (com.autocase.entity.ManualTestBatch batch : batches) {
                        List<com.autocase.entity.ManualTestDetail> details = manualTestDao.getDetailsByBatchId(batch.getBatchId());
                        for (com.autocase.entity.ManualTestDetail detail : details) {
                            if (detail.getCaseId() != null) {
                                groupCaseIds.add(detail.getCaseId());
                            }
                        }
                    }
                }
                final java.util.Set<String> targetIds = groupCaseIds;
                filtered = new ArrayList<>();
                for (TestCase tc : allCases) {
                    if (targetIds.contains(tc.getCasesID())) filtered.add(tc);
                }
            }
        }

        // 更新用例下拉框（不关联选项始终在首位）
        String currentSelection = caseCb.getValue();
        caseCb.getItems().clear();
        caseCb.getItems().add(noCaseOption);
        for (TestCase tc : filtered) {
            caseCb.getItems().add(tc.getCasesID() + " - " + tc.getCaseName());
        }
        // 恢复之前选中项（如果还在列表中）
        if (currentSelection != null && caseCb.getItems().contains(currentSelection)) {
            caseCb.setValue(currentSelection);
        } else if (!caseCb.getItems().isEmpty()) {
            caseCb.setValue(caseCb.getItems().get(0));
        }
    }

    private TestCase findCaseById(String caseId) {
        if (rootDirectory == null) return null;
        List<TestCase> cases = caseDao.scanCases(rootDirectory);
        for (TestCase tc : cases) {
            if (caseId.equals(tc.getCasesID())) return tc;
        }
        return null;
    }

    private static String getExtension(String language) {
        switch (language) {
            case "python":   return ".py";
            case "java":     return ".java";
            case "cpp":      return ".cpp";
            case "csharp":   return ".cs";
            case "gdscript": return ".gd";
            default:         return ".txt";
        }
    }

    private static String sanitizeFileName(String name) {
        if (name == null) return "test";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 从模板文本中提取 CASE_NAME 的值（用于不关联用例时判断是否需要询问文件名）
     */
    private static String extractCaseNameFromTemplate(String templateText, String language) {
        if (templateText == null || templateText.isEmpty()) return null;
        // 根据不同语言的 CASE_NAME 定义模式提取值
        String pattern;
        switch (language) {
            case "python":
                pattern = "CASE_NAME\\s*=\\s*\"([^\"]*)\"";
                break;
            case "java":
                pattern = "CASE_NAME\\s*=\\s*\"([^\"]*)\"";
                break;
            case "cpp":
                pattern = "CASE_NAME\\s*=\\s*\"([^\"]*)\"";
                break;
            case "csharp":
                pattern = "CaseName\\s*=\\s*\"([^\"]*)\"";
                break;
            case "gdscript":
                pattern = "case_name\\s*:\\s*String\\s*=\\s*\"([^\"]*)\"";
                break;
            default:
                return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(templateText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 将内容写入输出文件并提示成功
     */
    private void writeOutputFile(File outputFile, String content) {
        try {
            java.io.FileWriter fw = new java.io.FileWriter(outputFile);
            fw.write(content);
            fw.close();
            DialogUtil.showInfo("已生成: " + outputFile.getAbsolutePath());
        } catch (java.io.IOException ex) {
            DialogUtil.showError("生成失败: " + ex.getMessage());
        }
    }
}

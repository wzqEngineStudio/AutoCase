package com.autocase.ui;

import com.autocase.dao.ConfigDao;
import com.autocase.entity.ManualTestBatch;
import com.autocase.entity.ReportData;
import com.autocase.entity.TestCase;
import com.autocase.logic.CaseLogic;
import com.autocase.util.DialogUtil;
import com.autocase.util.LayoutPersistence;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * 主窗口
 */
public class MainWindow {

    private final Stage primaryStage;
    private final CaseLogic caseLogic;
    private final ConfigDao configDao;
    private ConfigDao.CmsConfig config;
    private ResourceExplorer resourceExplorer;
    private CaseFilterPanel caseFilterPanel;
    private GithubPanel githubPanel;
    private ManualTestPanel manualTestPanel;
    private AutoTestPanel autoTestPanel;
    private ReportPanel reportPanel;
    private DefectPanel defectPanel;
    private SettingsPanel settingsPanel;
    private TabPane tabPane;
    private String rootDirectory;
    private String currentViewedCasePath;
    private Label dirLabel;
    private ComboBox<String> historyCombo;

    public MainWindow(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.caseLogic = new CaseLogic();
        this.configDao = new ConfigDao();
    }

    public void show() {
        primaryStage.setTitle("AutoCase - 自动化测试用例管理系统");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);

        BorderPane root = new BorderPane();

        // 顶部工具栏
        root.setTop(createToolBar());

        // 左侧资源管理器
        resourceExplorer = new ResourceExplorer();
        resourceExplorer.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            String filePath = resourceExplorer.getSelectedFilePath();
            if (filePath != null && filePath.toLowerCase().endsWith("case.json")) {
                loadAndShowCase(filePath);
            }
        });

        // 文件系统变化时自动刷新
        resourceExplorer.setOnFileSystemChanged(() -> {
            if (rootDirectory != null) {
                caseLogic.loadCases(rootDirectory);
                caseFilterPanel.refreshTable();
                caseFilterPanel.updateSeverityOptions();
                caseFilterPanel.updatePriorityOptions();
            }
        });

        // 右键新建用例回调
        resourceExplorer.setOnCreateCase(this::createNewCaseInDirectory);

        // 右侧用例筛选面板
        caseFilterPanel = new CaseFilterPanel(caseLogic);
        caseFilterPanel.setOnEditCase(this::editCase);
        caseFilterPanel.setOnDeleteCase(this::deleteCase);
        caseFilterPanel.setOnAddToTask(this::addToTask);

        // GitHub面板
        githubPanel = new GithubPanel(configDao, primaryStage);
        githubPanel.setOnPushRequested(() -> {
            if (rootDirectory == null) {
                DialogUtil.showError(primaryStage, "请先选择根目录");
                return;
            }
            githubPanel.doPush(rootDirectory);
        });

        // 手工测试面板
        manualTestPanel = new ManualTestPanel();

        // 自动化测试面板
        autoTestPanel = new AutoTestPanel();
        autoTestPanel.setOnReportGenerated(this::onReportGenerated);

        // 报告面板
        reportPanel = new ReportPanel();

        // 执行历史面板
        ExecutionHistoryPanel historyPanel = new ExecutionHistoryPanel();

        // 缺陷管理面板
        defectPanel = new DefectPanel();

        // 设置面板
        settingsPanel = new SettingsPanel();

        // 主内容区：用例面板 + GitHub面板 + 自动化测试 + 测试报告 + 执行历史
        tabPane = new TabPane();

        Tab caseTab = new Tab("用例管理");
        caseTab.setClosable(false);
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(resourceExplorer, caseFilterPanel);
        double[] mainLayout = LayoutPersistence.getPositions("MainWindow", new double[]{0.25});
        splitPane.setDividerPositions(mainLayout);
        splitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            LayoutPersistence.savePositions("MainWindow", splitPane.getDividerPositions());
        });
        caseTab.setContent(splitPane);

        Tab githubTab = new Tab("Git仓库");
        githubTab.setClosable(false);
        githubTab.setContent(githubPanel);

        Tab manualTestTab = new Tab("手工测试");
        manualTestTab.setClosable(false);
        manualTestTab.setContent(manualTestPanel);

        Tab autoTestTab = new Tab("自动化测试");
        autoTestTab.setClosable(false);
        autoTestTab.setContent(autoTestPanel);

        Tab reportTab = new Tab("测试报告");
        reportTab.setClosable(false);
        reportTab.setContent(reportPanel);

        Tab historyTab = new Tab("执行历史");
        historyTab.setClosable(false);
        historyTab.setContent(historyPanel);

        Tab defectTab = new Tab("缺陷管理");
        defectTab.setClosable(false);
        defectTab.setContent(defectPanel);

        Tab settingsTab = new Tab("设置");
        settingsTab.setClosable(false);
        settingsTab.setContent(settingsPanel);

        tabPane.getTabs().addAll(caseTab, githubTab, manualTestTab, autoTestTab, reportTab, historyTab, defectTab, settingsTab);

        root.setCenter(tabPane);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        // 加载配置并恢复上次状态
        loadConfigAndRestore();

        // 窗口关闭时保存配置
        primaryStage.setOnCloseRequest(e -> saveConfig());

        primaryStage.show();
    }

    /**
     * 加载配置并恢复上次状态
     */
    private void loadConfigAndRestore() {
        config = configDao.loadConfig();

        // 刷新历史下拉框
        refreshHistoryCombo();

        // 恢复上次打开的目录
        if (config.getLastRootDirectory() != null) {
            File dir = new File(config.getLastRootDirectory());
            if (dir.exists() && dir.isDirectory()) {
                rootDirectory = config.getLastRootDirectory();
                dirLabel.setText("当前目录: " + rootDirectory);
                refreshData();

                // 恢复上次查看的用例
                if (config.getLastViewedCasePath() != null) {
                    currentViewedCasePath = config.getLastViewedCasePath();
                    // 延迟执行，等待表格数据加载完成
                    javafx.application.Platform.runLater(() -> restoreLastViewedCase());
                }
            }
        }
    }

    /**
     * 恢复上次查看的用例
     */
    private void restoreLastViewedCase() {
        if (currentViewedCasePath == null) {
            return;
        }

        // 在表格中查找并选中该用例
        caseFilterPanel.selectCaseByPath(currentViewedCasePath);

        // 在资源管理器中展开该文件（不触发选中事件，避免弹出编辑对话框）
        resourceExplorer.expandToPath(currentViewedCasePath, false);
    }

    /**
     * 保存配置
     */
    private void saveConfig() {
        System.out.println("[DEBUG] saveConfig called");
        System.out.println("[DEBUG] rootDirectory = " + rootDirectory);
        System.out.println("[DEBUG] currentViewedCasePath = " + currentViewedCasePath);

        // 先重新加载配置，避免覆盖其他模块（如AutoTestPanel）保存的数据
        config = configDao.loadConfig();
        
        config.setLastRootDirectory(rootDirectory);
        config.setLastViewedCasePath(currentViewedCasePath);
        config.setLastViewedScriptPath(autoTestPanel.getLastViewedScriptPath());

        boolean success = configDao.saveConfig(config);
        System.out.println("[DEBUG] saveConfig result = " + success);
    }

    /**
     * 创建顶部工具栏
     */
    private HBox createToolBar() {
        HBox toolBar = new HBox(10);
        toolBar.setPadding(new Insets(8, 10, 8, 10));
        toolBar.setStyle("-fx-background-color: #f0f0f0;");
        toolBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button selectDirBtn = new Button("选择根目录");

        // 目录历史下拉框
        historyCombo = new ComboBox<>();
        historyCombo.setPromptText("最近打开的目录");
        historyCombo.setMaxWidth(300);
        historyCombo.setDisable(true);
        historyCombo.setOnAction(e -> {
            String selectedDir = historyCombo.getValue();
            if (selectedDir != null && !selectedDir.isEmpty()) {
                openDirectory(selectedDir);
            }
        });

        dirLabel = new Label("未选择目录");
        dirLabel.setStyle("-fx-font-weight: bold;");

        // GitHub Octocat图标按钮
        Button githubBtn = GithubPanel.createGithubButton();
        githubBtn.setOnAction(e -> {
            // 切换到Git仓库Tab
            if (tabPane != null) {
                tabPane.getSelectionModel().select(1);
            }
        });

        selectDirBtn.setOnAction(e -> selectDirectory());

        toolBar.getChildren().addAll(selectDirBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                historyCombo,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                githubBtn,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                dirLabel);
        HBox.setHgrow(dirLabel, Priority.ALWAYS);

        return toolBar;
    }

    /**
     * 选择根目录
     */
    private void selectDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择用例根目录");

        if (rootDirectory != null) {
            chooser.setInitialDirectory(new File(rootDirectory));
        }

        File selectedDir = chooser.showDialog(primaryStage);
        if (selectedDir != null) {
            openDirectory(selectedDir.getAbsolutePath());
        }
    }

    /**
     * 打开指定目录
     */
    private void openDirectory(String directory) {
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            DialogUtil.showError(primaryStage, "目录不存在: " + directory);
            return;
        }

        rootDirectory = directory;
        dirLabel.setText("当前目录: " + rootDirectory);
        currentViewedCasePath = null;

        // 添加到历史记录
        config.addToDirectoryHistory(directory);
        refreshHistoryCombo();

        refreshData();
    }

    /**
     * 刷新历史下拉框
     */
    private void refreshHistoryCombo() {
        if (historyCombo == null) {
            return;
        }
        historyCombo.getItems().clear();
        if (config != null && config.getDirectoryHistory() != null && !config.getDirectoryHistory().isEmpty()) {
            for (String dir : config.getDirectoryHistory()) {
                historyCombo.getItems().add(dir);
            }
            historyCombo.setDisable(false);
        } else {
            historyCombo.setDisable(true);
        }
    }

    /**
     * 刷新数据
     */
    private void refreshData() {
        if (rootDirectory == null) {
            return;
        }

        caseLogic.loadCases(rootDirectory);
        resourceExplorer.setRootDirectory(rootDirectory);
        autoTestPanel.setRootDirectory(rootDirectory);
        manualTestPanel.setRootDirectory(rootDirectory);
        caseFilterPanel.refreshTable();
        caseFilterPanel.updateSeverityOptions();
        caseFilterPanel.updatePriorityOptions();
    }

    /**
     * 加载并显示用例
     */
    private void loadAndShowCase(String filePath) {
        TestCase testCase = caseLogic.getAllCases().stream()
                .filter(c -> filePath.equals(c.getFilePath()))
                .findFirst()
                .orElse(null);

        if (testCase != null) {
            currentViewedCasePath = filePath;
            editCase(testCase);
        }
    }

    /**
     * 新建用例（在根目录下）
     */
    private void createNewCase() {
        if (rootDirectory == null) {
            DialogUtil.showError(primaryStage, "请先选择根目录");
            return;
        }
        createNewCaseInDirectory(rootDirectory);
    }

    /**
     * 在指定目录下新建用例
     */
    private void createNewCaseInDirectory(String directory) {
        CaseEditDialog dialog = new CaseEditDialog(null, directory, caseLogic.getAllCases(), true);
        dialog.showAndWait();

        if (dialog.getResult()) {
            TestCase newCase = dialog.getEditedCase();
            boolean success = caseLogic.createCase(directory, newCase);
            if (success) {
                refreshData();
                DialogUtil.showInfo(primaryStage, "新建成功");
            } else {
                DialogUtil.showError(primaryStage, "新建失败");
            }
        }
    }

    /**
     * 编辑用例
     */
    private void editCase(TestCase testCase) {
        CaseEditDialog dialog = new CaseEditDialog(testCase, rootDirectory, caseLogic.getAllCases(), false);
        dialog.showAndWait();

        if (dialog.getResult()) {
            TestCase editedCase = dialog.getEditedCase();
            editedCase.setFilePath(testCase.getFilePath());
            boolean success = caseLogic.saveCase(editedCase);
            if (success) {
                refreshData();
                DialogUtil.showInfo(primaryStage, "保存成功");
            } else {
                DialogUtil.showError(primaryStage, "保存失败");
            }
        }
    }

    /**
     * 删除用例
     */
    private void deleteCase(TestCase testCase) {
        String caseName = testCase.getCaseInfo().getCaseName();
        if (DialogUtil.showConfirm(primaryStage, "确认删除", "确定要删除用例: " + caseName + "?", "此操作不可恢复")) {
            boolean success = caseLogic.deleteCase(testCase);
            if (success) {
                if (currentViewedCasePath != null && currentViewedCasePath.equals(testCase.getFilePath())) {
                    currentViewedCasePath = null;
                }
                refreshData();
                DialogUtil.showInfo(primaryStage, "删除成功");
            } else {
                DialogUtil.showError(primaryStage, "删除失败");
            }
        }
    }

    /**
     * 添加用例到任务
     */
    private void addToTask(TestCase testCase) {
        List<ManualTestBatch> availableTasks = manualTestPanel.getAvailableTasks();

        Dialog<ManualTestBatch> dialog = new Dialog<>();
        dialog.setTitle("添加到任务");
        dialog.setHeaderText("选择要添加到的任务（用例 \"" + testCase.getCaseInfo().getCaseName() + "\"）");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // 任务列表（排除 FromManager）
        ListView<ManualTestBatch> taskListView = new ListView<>();
        taskListView.getItems().addAll(availableTasks);
        // 修复：空列表时点击导致IndexOutOfBoundsException
        DialogUtil.fixListViewEmptyClickBug(taskListView);
        taskListView.setCellFactory(lv -> new ListCell<ManualTestBatch>() {
            @Override
            protected void updateItem(ManualTestBatch item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTaskName() + " - " + item.getModuleName());
                }
            }
        });

        // 新建任务按钮
        Button newTaskBtn = new Button("新建任务");
        newTaskBtn.setOnAction(e -> {
            dialog.setResult(null); // 关闭对话框
            dialog.hide();
            // 触发新建任务流程
            createTaskAndAddCase(testCase);
        });

        HBox buttonBox = new HBox(10, newTaskBtn);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        content.getChildren().addAll(new Label("已有任务:"), taskListView, buttonBox);
        dialog.getDialogPane().setContent(content);

        ButtonType okType = new ButtonType("添加到选中任务", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == okType) {
                return taskListView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selectedTask -> {
            if (selectedTask != null) {
                boolean success = manualTestPanel.addCaseToTask(selectedTask.getBatchId(), testCase);
                if (success) {
                    DialogUtil.showInfo(primaryStage, "已添加到任务: " + selectedTask.getTaskName());
                } else {
                    DialogUtil.showError(primaryStage, "添加失败");
                }
            }
        });
    }

    /**
     * 新建任务并添加用例
     */
    private void createTaskAndAddCase(TestCase testCase) {
        Dialog<ManualTestBatch> dialog = new Dialog<>();
        dialog.setTitle("新建任务");
        dialog.setHeaderText("创建新任务并添加用例");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField taskNameField = new TextField();
        TextField versionField = new TextField();
        TextField moduleField = new TextField();
        TextField testerField = new TextField();

        grid.add(new Label("任务名称:"), 0, 0);
        grid.add(taskNameField, 1, 0);
        grid.add(new Label("版本:"), 0, 1);
        grid.add(versionField, 1, 1);
        grid.add(new Label("功能模块:"), 0, 2);
        grid.add(moduleField, 1, 2);
        grid.add(new Label("测试人:"), 0, 3);
        grid.add(testerField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        ButtonType createType = new ButtonType("创建并添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == createType) {
                ManualTestBatch batch = new ManualTestBatch();
                batch.setTaskName(taskNameField.getText().trim());
                batch.setVersion(versionField.getText().trim());
                batch.setModuleName(moduleField.getText().trim());
                batch.setTester(testerField.getText().trim());
                batch.setTotalCases(1);
                batch.setPassedCount(0);
                batch.setFailedCount(0);
                batch.setBlockedCount(0);
                batch.setPendingCount(1);
                batch.setStartTime(new java.sql.Timestamp(System.currentTimeMillis()));
                return batch;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(batch -> {
            Long batchId = manualTestPanel.addCaseToTaskReturnId(batch, testCase);
            if (batchId != null) {
                DialogUtil.showInfo(primaryStage, "任务创建成功，已添加用例");
            } else {
                DialogUtil.showError(primaryStage, "添加失败");
            }
        });
    }

    /**
     * 报告生成回调
     */
    private void onReportGenerated(ReportData report) {
        reportPanel.showReport(report);
        // 刷新用例管理面板（状态已更新到JSON文件）
        caseLogic.loadCases(rootDirectory);
        caseFilterPanel.refreshTable();
        // 切换到报告Tab
        tabPane.getSelectionModel().select(3);
    }
}

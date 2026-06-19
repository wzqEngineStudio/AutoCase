package com.autocase.ui;

import com.autocase.dao.ConfigDao;
import com.autocase.dao.ExecutionHistoryDao;
import com.autocase.dao.ManualTestDao;
import com.autocase.dao.ReportDao;
import com.autocase.entity.*;
import com.autocase.util.DialogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

/**
 * 测试报告面板 - 包含自动化测试和手工测试两个子模块
 */
public class ReportPanel extends BorderPane {

    private final ReportDao reportDao;
    private final ConfigDao configDao;
    private final ExecutionHistoryDao historyDao;
    private final ManualTestDao manualTestDao;
    private ConfigDao.CmsConfig config;
    private ReportData currentReport;

    // 自动化统计标签
    private Label totalLabel;
    private Label executedLabel;
    private Label passedLabel;
    private Label failedLabel;
    private Label blockedLabel;
    private Label timeoutLabel;
    private Label skippedLabel;
    private Label notExecutedLabel;
    private Label passRateLabel;
    private Label durationLabel;
    private Label baselineLabel;

    // 手工统计标签
    private Label manualTotalLabel;
    private Label manualExecutedLabel;
    private Label manualPassedLabel;
    private Label manualFailedLabel;
    private Label manualBlockedLabel;
    private Label manualPendingLabel;
    private Label manualPassRateLabel;

    // 结果表格
    private TableView<ExecutionResult> resultTable;
    private TableView<ManualTestDetail> manualResultTable;

    public ReportPanel() {
        this.reportDao = new ReportDao();
        this.configDao = new ConfigDao();
        this.historyDao = new ExecutionHistoryDao();
        this.manualTestDao = new ManualTestDao();
        this.config = configDao.loadConfig();
        buildUI();

        // 启动时加载上次报告
        loadLastReport();
    }

    private void buildUI() {
        // 中间：双Tab（自动化测试 / 手工测试），每个Tab自带统计面板
        TabPane tabPane = new TabPane();

        // 自动化测试Tab（独立页面：统计 + 表格 + 详情）
        Tab autoTab = new Tab("自动化测试");
        autoTab.setClosable(false);
        VBox autoStatsBox = createAutoStatsPanel();
        resultTable = createAutoResultTable();
        ScrollPane autoScroll = new ScrollPane(resultTable);
        autoScroll.setFitToWidth(true);
        VBox autoDetailBox = createAutoDetailPanel();
        VBox autoBox = new VBox(5);
        autoBox.getChildren().addAll(autoStatsBox, autoScroll, autoDetailBox);
        VBox.setVgrow(autoScroll, Priority.ALWAYS);
        autoTab.setContent(autoBox);

        // 手工测试Tab（独立页面：统计 + 表格 + 详情）
        Tab manualTab = new Tab("手工测试");
        manualTab.setClosable(false);
        VBox manualStatsBox = createManualStatsPanel();
        manualResultTable = createManualResultTable();
        ScrollPane manualScroll = new ScrollPane(manualResultTable);
        manualScroll.setFitToWidth(true);
        VBox manualDetailBox = createManualDetailPanel();
        VBox manualBox = new VBox(5);
        manualBox.getChildren().addAll(manualStatsBox, manualScroll, manualDetailBox);
        VBox.setVgrow(manualScroll, Priority.ALWAYS);
        manualTab.setContent(manualBox);

        tabPane.getTabs().addAll(autoTab, manualTab);

        // 底部：操作按钮
        HBox buttonBox = createButtonBox();

        setCenter(tabPane);
        setBottom(buttonBox);
    }

    /**
     * 创建自动化测试独立的统计面板（放在自动化Tab内部顶部）
     */
    private VBox createAutoStatsPanel() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8, 10, 5, 10));
        box.setStyle("-fx-background-color: #E3F2FD; -fx-border-color: #BBDEFB; -fx-border-width: 0 0 1 0;");

        HBox titleRow = new HBox(10);
        Label titleLabel = new Label("自动化测试");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1565C0;");
        baselineLabel = createStatLabel("");
        baselineLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        titleRow.getChildren().addAll(titleLabel, spacer, baselineLabel);

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15);
        statsGrid.setVgap(3);

        totalLabel = createStatLabel("总脚本数: 0");
        executedLabel = createStatLabel("已执行: 0");
        passedLabel = createStatLabel("通过: 0", "-fx-text-fill: green;");
        failedLabel = createStatLabel("失败: 0", "-fx-text-fill: red;");
        blockedLabel = createStatLabel("阻塞: 0", "-fx-text-fill: orange;");
        timeoutLabel = createStatLabel("超时: 0", "-fx-text-fill: #cc0000;");
        skippedLabel = createStatLabel("跳过: 0", "-fx-text-fill: gray;");
        notExecutedLabel = createStatLabel("未执行: 0");
        passRateLabel = createStatLabel("通过率: 0%", "-fx-font-weight: bold;");
        durationLabel = createStatLabel("耗时: 0s");

        statsGrid.add(totalLabel, 0, 0);
        statsGrid.add(executedLabel, 1, 0);
        statsGrid.add(passedLabel, 2, 0);
        statsGrid.add(failedLabel, 3, 0);
        statsGrid.add(blockedLabel, 4, 0);
        statsGrid.add(timeoutLabel, 0, 1);
        statsGrid.add(skippedLabel, 1, 1);
        statsGrid.add(notExecutedLabel, 2, 1);
        statsGrid.add(passRateLabel, 3, 1);
        statsGrid.add(durationLabel, 4, 1);

        box.getChildren().addAll(titleRow, statsGrid);
        return box;
    }

    /**
     * 创建手工测试独立的统计面板（放在手工Tab内部顶部）
     */
    private VBox createManualStatsPanel() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8, 10, 5, 10));
        box.setStyle("-fx-background-color: #E8F5E9; -fx-border-color: #C8E6C9; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("手工测试");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15);
        statsGrid.setVgap(3);

        manualTotalLabel = createStatLabel("总用例数: 0");
        manualExecutedLabel = createStatLabel("已执行: 0");
        manualPassedLabel = createStatLabel("通过: 0", "-fx-text-fill: green;");
        manualFailedLabel = createStatLabel("失败: 0", "-fx-text-fill: red;");
        manualBlockedLabel = createStatLabel("阻塞: 0", "-fx-text-fill: orange;");
        manualPendingLabel = createStatLabel("待执行: 0");
        manualPassRateLabel = createStatLabel("通过率: 0%", "-fx-font-weight: bold;");

        statsGrid.add(manualTotalLabel, 0, 0);
        statsGrid.add(manualExecutedLabel, 1, 0);
        statsGrid.add(manualPassedLabel, 2, 0);
        statsGrid.add(manualFailedLabel, 3, 0);
        statsGrid.add(manualBlockedLabel, 4, 0);
        statsGrid.add(manualPendingLabel, 0, 1);
        statsGrid.add(manualPassRateLabel, 1, 1);

        box.getChildren().addAll(titleLabel, statsGrid);
        return box;
    }

    private Label createStatLabel(String text) {
        return createStatLabel(text, null);
    }

    private Label createStatLabel(String text, String style) {
        Label label = new Label(text);
        if (style != null) {
            label.setStyle(style);
        }
        return label;
    }

    private TableView<ExecutionResult> createAutoResultTable() {
        TableView<ExecutionResult> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ExecutionResult, String> nameCol = new TableColumn<>("脚本名称");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getScriptName()));

        TableColumn<ExecutionResult, String> langCol = new TableColumn<>("语言");
        langCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getLanguage()));

        TableColumn<ExecutionResult, String> caseCol = new TableColumn<>("关联用例");
        caseCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getTestCasePath() != null ? "已关联" : "未关联"));

        TableColumn<ExecutionResult, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<ExecutionResult, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "PASSED": setStyle("-fx-text-fill: green; -fx-font-weight: bold;"); break;
                        case "FAILED": setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); break;
                        case "BLOCKED": setStyle("-fx-text-fill: orange; -fx-font-weight: bold;"); break;
                        case "TIMEOUT": setStyle("-fx-text-fill: #cc0000; -fx-font-weight: bold;"); break;
                        case "SKIPPED": setStyle("-fx-text-fill: gray; -fx-font-weight: bold;"); break;
                        default: setStyle("");
                    }
                }
            }
        });

        TableColumn<ExecutionResult, String> priorityCol = new TableColumn<>("优先级");
        priorityCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getPriority() != null ? data.getValue().getPriority() : "-"));
        priorityCol.setCellFactory(col -> new TableCell<ExecutionResult, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "-".equals(item)) {
                    setText(empty ? null : item);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "P0": case "Critical": setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); break;
                        case "P1": case "High": setStyle("-fx-text-fill: #e65100; -fx-font-weight: bold;"); break;
                        case "P2": case "Medium": setStyle("-fx-text-fill: #1976D2;"); break;
                        case "P3": case "Low": setStyle("-fx-text-fill: green;"); break;
                        default: setStyle("");
                    }
                }
            }
        });

        TableColumn<ExecutionResult, String> severityCol = new TableColumn<>("严重程度");
        severityCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getSeverity() != null ? data.getValue().getSeverity() : "-"));
        severityCol.setCellFactory(col -> new TableCell<ExecutionResult, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "-".equals(item)) {
                    setText(empty ? null : item);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "S1": case "Fatal": setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); break;
                        case "S2": case "Critical": setStyle("-fx-text-fill: #e65100; -fx-font-weight: bold;"); break;
                        case "S3": case "Major": setStyle("-fx-text-fill: #1976D2;"); break;
                        case "S4": case "Minor": setStyle("-fx-text-fill: green;"); break;
                        default: setStyle("");
                    }
                }
            }
        });

        TableColumn<ExecutionResult, String> timeCol = new TableColumn<>("耗时(ms)");
        timeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().getExecutionTime())));

        TableColumn<ExecutionResult, String> errorCol = new TableColumn<>("错误信息");
        errorCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getErrorMessage() != null ? data.getValue().getErrorMessage() : ""));

        // 操作列：为失败用例添加"创建手工任务"和"添加到任务"按钮
        TableColumn<ExecutionResult, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(160);
        actionCol.setStyle("-fx-alignment: CENTER;");
        actionCol.setCellFactory(param -> new TableCell<ExecutionResult, Void>() {
            private final Button createTaskBtn = new Button("创建手工任务");
            private final Button addToTaskBtn = new Button("添加到任务");
            private final HBox buttonBox = new HBox(4, createTaskBtn, addToTaskBtn);

            {
                createTaskBtn.setStyle("-fx-font-size: 11px;");
                createTaskBtn.setOnAction(e -> {
                    ExecutionResult result = getTableView().getItems().get(getIndex());
                    createManualTaskFromFailed(result);
                });

                addToTaskBtn.setStyle("-fx-font-size: 11px;");
                addToTaskBtn.setOnAction(e -> {
                    ExecutionResult result = getTableView().getItems().get(getIndex());
                    addFailedCaseToExistingTask(result);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ExecutionResult result = getTableView().getItems().get(getIndex());
                    // 仅对失败/阻塞/超时的用例显示按钮
                    String status = result.getStatus();
                    if ("FAILED".equals(status) || "BLOCKED".equals(status) || "TIMEOUT".equals(status)) {
                        createTaskBtn.setDisable(false);
                        addToTaskBtn.setDisable(false);
                        setGraphic(buttonBox);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        table.getColumns().addAll(nameCol, langCol, caseCol, statusCol, priorityCol, severityCol, timeCol, errorCol, actionCol);

        // 选中行时从 H2 加载详情
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadDetailFromH2(newVal);
            }
        });

        return table;
    }

    /**
     * 确保存在"自动化转手工"默认任务组，返回其ID
     */
    private Long ensureAutoConvertGroupExists() {
        TaskGroup group = manualTestDao.getTaskGroupByName("自动化转手工");
        if (group == null) {
            group = new TaskGroup();
            group.setGroupName("自动化转手工");
            group.setDescription("从测试报告的失败用例自动创建的手工任务");
            group.setRemark("系统保留任务组");
            group.setCreateTime(new Timestamp(System.currentTimeMillis()));
            Long gid = manualTestDao.insertTaskGroup(group);
            return gid;
        }
        return group.getGroupId();
    }

    /**
     * 从失败的自动化用例创建手工任务
     */
    private void createManualTaskFromFailed(ExecutionResult result) {
        Dialog<ManualTestBatch> dialog = new Dialog<>();
        dialog.setTitle("从自动化失败创建手工任务");
        dialog.setHeaderText("为用例 [" + result.getCaseName() + "] 创建手工验证任务");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField taskNameField = new TextField("手工验证_" + result.getCaseName());
        TextField versionField = new TextField();
        versionField.setPromptText("版本号");
        TextField moduleField = new TextField();
        moduleField.setPromptText("模块名");
        TextField testerField = new TextField();
        testerField.setPromptText("测试人");
        TextArea remarkArea = new TextArea();
        remarkArea.setPrefRowCount(3);
        remarkArea.setWrapText(true);
        remarkArea.setPromptText("备注（自动带入自动化执行上下文）");

        // 自动带入自动化执行上下文
        StringBuilder autoContext = new StringBuilder();
        autoContext.append("自动化执行失败，转入手工验证。\n");
        autoContext.append("自动化用例ID: ").append(result.getCaseName()).append("\n");
        autoContext.append("自动化状态: ").append(result.getStatus()).append("\n");
        autoContext.append("自动化错误: ").append(result.getErrorMessage() != null ? result.getErrorMessage() : "无").append("\n");
        autoContext.append("自动化耗时: ").append(result.getExecutionTime()).append("ms\n");
        if (result.getTestCasePath() != null) {
            autoContext.append("关联用例路径: ").append(result.getTestCasePath()).append("\n");
        }
        remarkArea.setText(autoContext.toString());

        grid.add(new Label("任务名称:"), 0, 0);
        grid.add(taskNameField, 1, 0);
        grid.add(new Label("版本:"), 0, 1);
        grid.add(versionField, 1, 1);
        grid.add(new Label("模块:"), 0, 2);
        grid.add(moduleField, 1, 2);
        grid.add(new Label("测试人:"), 0, 3);
        grid.add(testerField, 1, 3);
        grid.add(new Label("备注:"), 0, 4);
        grid.add(remarkArea, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                ManualTestBatch batch = new ManualTestBatch();
                batch.setTaskName(taskNameField.getText().trim());
                batch.setVersion(versionField.getText().trim());
                batch.setModuleName(moduleField.getText().trim());
                batch.setTester(testerField.getText().trim());
                batch.setRemark(remarkArea.getText().trim());
                batch.setStartTime(new Timestamp(System.currentTimeMillis()));
                return batch;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(batch -> {
            // 确保有一个默认任务组来容纳自动化转手工的任务
            Long defaultGroupId = ensureAutoConvertGroupExists();
            batch.setGroupId(defaultGroupId);

            Long batchId = manualTestDao.insertBatch(batch);
            if (batchId != null) {
                // 添加该用例到手工任务
                ManualTestDetail detail = new ManualTestDetail();
                detail.setBatchId(batchId);
                detail.setCaseId(result.getCaseName());
                detail.setCaseName(result.getCaseName());
                detail.setStatus("PENDING");
                detail.setPriority(result.getPriority());
                detail.setSeverity(result.getSeverity());
                detail.setExpectedResult(result.getErrorMessage() != null ? "需手工验证自动化失败原因: " + result.getErrorMessage() : "需手工验证");
                detail.setRemark("从自动化报告创建，自动化状态: " + result.getStatus());
                detail.setLinkedAutoCaseId(result.getCaseName());
                detail.setExecutionTime(new Timestamp(System.currentTimeMillis()));
                manualTestDao.insertDetail(detail);

                batch.setBatchId(batchId);
                batch.setTotalCases(1);
                batch.setPendingCount(1);
                manualTestDao.updateBatch(batch);

                DialogUtil.showInfo("手工任务已创建，用例已添加");
                updateManualStats();
            }
        });
    }

    /**
     * 将失败的自动化用例添加到已有的手工测试任务中
     */
    private void addFailedCaseToExistingTask(ExecutionResult result) {
        // 获取所有已有任务（排除FromManager任务组下的任务）
        List<ManualTestBatch> allBatches = manualTestDao.getAllBatches();
        List<ManualTestBatch> availableTasks = allBatches.stream()
                .filter(b -> {
                    if (b.getGroupId() == null) return true;
                    TaskGroup g = manualTestDao.getTaskGroupById(b.getGroupId());
                    return g == null || !"FromManager".equals(g.getGroupName());
                })
                .toList();

        if (availableTasks.isEmpty()) {
            DialogUtil.showInfo("暂无可用任务，请先创建一个手工测试任务");
            return;
        }

        Dialog<ManualTestBatch> dialog = new Dialog<>();
        dialog.setTitle("添加到已有任务");
        dialog.setHeaderText("将用例 [" + result.getCaseName() + "] 添加到哪个已有任务？");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label infoLabel = new Label("自动化状态: " + result.getStatus()
                + (result.getErrorMessage() != null ? " | 错误: " + result.getErrorMessage() : ""));
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        ListView<ManualTestBatch> taskListView = new ListView<>();
        taskListView.getItems().addAll(availableTasks);
        // 修复：空列表时点击导致IndexOutOfBoundsException
        DialogUtil.fixListViewEmptyClickBug(taskListView);
        taskListView.setPrefHeight(200);
        taskListView.setCellFactory(lv -> new ListCell<ManualTestBatch>() {
            @Override
            protected void updateItem(ManualTestBatch item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    int total = item.getTotalCases() != null ? item.getTotalCases() : 0;
                    int done = (item.getPassedCount() != null ? item.getPassedCount() : 0)
                            + (item.getFailedCount() != null ? item.getFailedCount() : 0)
                            + (item.getBlockedCount() != null ? item.getBlockedCount() : 0);
                    setText(item.getTaskName() + " (" + item.getModuleName() + ") - 进度: " + done + "/" + total);
                }
            }
        });

        content.getChildren().addAll(infoLabel, new Label("选择目标任务:"), taskListView);
        dialog.getDialogPane().setContent(content);

        ButtonType okType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == okType) {
                return taskListView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selectedTask -> {
            if (selectedTask == null) {
                DialogUtil.showError("请选择一个任务");
                return;
            }

            // 检查是否已存在
            List<ManualTestDetail> existingDetails = manualTestDao.getDetailsByBatchId(selectedTask.getBatchId());
            boolean alreadyExists = existingDetails.stream()
                    .anyMatch(d -> result.getCaseName() != null && result.getCaseName().equals(d.getCaseId()));
            if (alreadyExists) {
                DialogUtil.showInfo("该用例已存在于任务 [" + selectedTask.getTaskName() + "] 中");
                return;
            }

            // 添加到选中任务
            ManualTestDetail detail = new ManualTestDetail();
            detail.setBatchId(selectedTask.getBatchId());
            detail.setCaseId(result.getCaseName());
            detail.setCaseName(result.getCaseName());
            detail.setStatus("PENDING");
            detail.setPriority(result.getPriority());
            detail.setSeverity(result.getSeverity());
            detail.setExpectedResult(result.getErrorMessage() != null ? "需手工验证自动化失败原因: " + result.getErrorMessage() : "需手工验证");
            detail.setRemark("从自动化报告添加，自动化状态: " + result.getStatus());
            detail.setLinkedAutoCaseId(result.getCaseName());
            detail.setExecutionTime(new Timestamp(System.currentTimeMillis()));
            manualTestDao.insertDetail(detail);

            // 更新任务统计
            selectedTask.setTotalCases(selectedTask.getTotalCases() + 1);
            selectedTask.setPendingCount(selectedTask.getPendingCount() + 1);
            manualTestDao.updateBatch(selectedTask);

            updateManualStats();
            DialogUtil.showInfo("已将用例 [" + result.getCaseName() + "] 添加到任务 [" + selectedTask.getTaskName() + "]");
        });
    }

    private TableView<ManualTestDetail> createManualResultTable() {
        TableView<ManualTestDetail> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ManualTestDetail, String> caseIdCol = new TableColumn<>("用例ID");
        caseIdCol.setCellValueFactory(new PropertyValueFactory<>("caseId"));

        TableColumn<ManualTestDetail, String> caseNameCol = new TableColumn<>("用例名称");
        caseNameCol.setCellValueFactory(new PropertyValueFactory<>("caseName"));

        TableColumn<ManualTestDetail, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<ManualTestDetail, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "PASS": setStyle("-fx-text-fill: green; -fx-font-weight: bold;"); break;
                        case "FAIL": setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); break;
                        case "BLOCKED": setStyle("-fx-text-fill: orange; -fx-font-weight: bold;"); break;
                        case "PENDING": setStyle("-fx-text-fill: gray;"); break;
                        default: setStyle("");
                    }
                }
            }
        });

        TableColumn<ManualTestDetail, String> linkedCol = new TableColumn<>("关联自动化用例");
        linkedCol.setCellValueFactory(cell -> {
            String linked = cell.getValue().getLinkedAutoCaseId();
            return new javafx.beans.property.SimpleStringProperty(linked != null ? linked : "-");
        });

        TableColumn<ManualTestDetail, String> priorityCol = new TableColumn<>("优先级");
        priorityCol.setCellValueFactory(cell -> {
            String p = cell.getValue().getPriority();
            return new javafx.beans.property.SimpleStringProperty(p != null ? p : "-");
        });
        priorityCol.setCellFactory(col -> new TableCell<ManualTestDetail, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "-".equals(item)) {
                    setText(empty ? null : item);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "P0": case "Critical": setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); break;
                        case "P1": case "High": setStyle("-fx-text-fill: #e65100; -fx-font-weight: bold;"); break;
                        case "P2": case "Medium": setStyle("-fx-text-fill: #1976D2;"); break;
                        case "P3": case "Low": setStyle("-fx-text-fill: green;"); break;
                        default: setStyle("");
                    }
                }
            }
        });

        TableColumn<ManualTestDetail, String> severityCol = new TableColumn<>("严重程度");
        severityCol.setCellValueFactory(cell -> {
            String s = cell.getValue().getSeverity();
            return new javafx.beans.property.SimpleStringProperty(s != null ? s : "-");
        });
        severityCol.setCellFactory(col -> new TableCell<ManualTestDetail, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "-".equals(item)) {
                    setText(empty ? null : item);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "S1": case "Fatal": setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); break;
                        case "S2": case "Critical": setStyle("-fx-text-fill: #e65100; -fx-font-weight: bold;"); break;
                        case "S3": case "Major": setStyle("-fx-text-fill: #1976D2;"); break;
                        case "S4": case "Minor": setStyle("-fx-text-fill: green;"); break;
                        default: setStyle("");
                    }
                }
            }
        });

        TableColumn<ManualTestDetail, String> remarkCol = new TableColumn<>("备注");
        remarkCol.setCellValueFactory(cell -> {
            String r = cell.getValue().getRemark();
            return new javafx.beans.property.SimpleStringProperty(r != null ? r : "");
        });

        table.getColumns().addAll(caseIdCol, caseNameCol, statusCol, priorityCol, severityCol, linkedCol, remarkCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadManualDetailToPanel(newVal);
            }
        });

        return table;
    }

    private ComboBox<String> historyCombo;
    private TextArea detailArea;

    private VBox createAutoDetailPanel() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(5, 10, 5, 10));
        box.setStyle("-fx-background-color: #fafafa; -fx-border-color: #ddd; -fx-border-width: 1 0 0 0;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label historyLabel = new Label("执行历史:");
        historyCombo = new ComboBox<>();
        historyCombo.setPromptText("选择执行批次查看历史详情");
        historyCombo.setMaxWidth(300);
        historyCombo.setOnAction(e -> loadHistoryDetail());

        header.getChildren().addAll(historyLabel, historyCombo);

        detailArea = new TextArea();
        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setPrefHeight(120);
        detailArea.setPromptText("选择表格中的行或从下拉框选择执行批次查看详情");

        box.getChildren().addAll(header, detailArea);
        return box;
    }

    private TextArea manualDetailArea;
    private Label manualCaseIdLabel;
    private Label manualCaseNameLabel;
    private Label manualStatusLabel;
    private Label manualLinkedLabel;
    private TextArea manualExpectedArea;
    private TextArea manualActualArea;
    private TextArea manualReasonArea;

    private VBox createManualDetailPanel() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(5, 10, 5, 10));
        box.setStyle("-fx-background-color: #fafafa; -fx-border-color: #ddd; -fx-border-width: 1 0 0 0;");

        Label titleLabel = new Label("手工执行详情");
        titleLabel.setStyle("-fx-font-weight: bold;");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(5);

        manualCaseIdLabel = new Label("-");
        manualCaseNameLabel = new Label("-");
        manualStatusLabel = new Label("-");
        manualLinkedLabel = new Label("-");

        infoGrid.add(new Label("用例ID:"), 0, 0);
        infoGrid.add(manualCaseIdLabel, 1, 0);
        infoGrid.add(new Label("用例名称:"), 0, 1);
        infoGrid.add(manualCaseNameLabel, 1, 1);
        infoGrid.add(new Label("状态:"), 0, 2);
        infoGrid.add(manualStatusLabel, 1, 2);
        infoGrid.add(new Label("关联自动化:"), 0, 3);
        infoGrid.add(manualLinkedLabel, 1, 3);

        Label expectedLabel = new Label("期望结果:");
        manualExpectedArea = new TextArea();
        manualExpectedArea.setEditable(false);
        manualExpectedArea.setWrapText(true);
        manualExpectedArea.setPrefHeight(50);

        Label actualLabel = new Label("实际结果:");
        manualActualArea = new TextArea();
        manualActualArea.setEditable(false);
        manualActualArea.setWrapText(true);
        manualActualArea.setPrefHeight(50);

        Label reasonLabel = new Label("失败原因:");
        manualReasonArea = new TextArea();
        manualReasonArea.setEditable(false);
        manualReasonArea.setWrapText(true);
        manualReasonArea.setPrefHeight(50);

        box.getChildren().addAll(titleLabel, infoGrid, expectedLabel, manualExpectedArea, actualLabel, manualActualArea, reasonLabel, manualReasonArea);
        return box;
    }

    private void loadManualDetailToPanel(ManualTestDetail detail) {
        manualCaseIdLabel.setText(detail.getCaseId() != null ? detail.getCaseId() : "-");
        manualCaseNameLabel.setText(detail.getCaseName() != null ? detail.getCaseName() : "-");
        manualStatusLabel.setText(detail.getStatus() != null ? detail.getStatus() : "-");
        manualLinkedLabel.setText(detail.getLinkedAutoCaseId() != null ? detail.getLinkedAutoCaseId() : "-");
        manualExpectedArea.setText(detail.getExpectedResult() != null ? detail.getExpectedResult() : "");
        manualActualArea.setText(detail.getActualResult() != null ? detail.getActualResult() : "");
        manualReasonArea.setText(detail.getFailureReason() != null ? detail.getFailureReason() : "");
    }

    /**
     * 从 H2 加载选中行的执行详情
     */
    private void loadDetailFromH2(ExecutionResult result) {
        String caseName = result.getCaseName();
        if (caseName == null || caseName.isEmpty()) {
            detailArea.setText("该脚本未关联用例，无 H2 执行记录");
            return;
        }

        List<ExecutionDetail> details = historyDao.getDetailsByCaseId(caseName);
        if (details.isEmpty()) {
            detailArea.setText("未找到用例 [" + caseName + "] 的 H2 执行记录");
            return;
        }

        // 填充历史下拉框
        historyCombo.getItems().clear();
        for (ExecutionDetail d : details) {
            String item = String.format("批次#%d | %s | %s | %dms",
                d.getBatchId(), d.getStatus(),
                d.getExecutionTime() != null ? d.getExecutionTime().toString() : "N/A",
                d.getDurationMs());
            historyCombo.getItems().add(item);
        }

        // 默认显示最近一次
        showDetailContent(details.get(0));
    }

    /**
     * 从下拉框加载历史详情
     */
    private void loadHistoryDetail() {
        String selected = historyCombo.getValue();
        if (selected == null) return;

        // 从选中项解析 batchId
        int hashIdx = selected.indexOf('#');
        int pipeIdx = selected.indexOf('|');
        if (hashIdx < 0 || pipeIdx < 0) return;

        long batchId = Long.parseLong(selected.substring(hashIdx + 1, pipeIdx).trim());
        String caseName = resultTable.getSelectionModel().getSelectedItem().getCaseName();

        List<ExecutionDetail> details = historyDao.getDetailsByBatchId(batchId);
        for (ExecutionDetail d : details) {
            if (caseName != null && caseName.equals(d.getCaseId())) {
                showDetailContent(d);
                return;
            }
        }
    }

    /**
     * 显示详情内容
     */
    private void showDetailContent(ExecutionDetail detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("【用例ID】 ").append(detail.getCaseId()).append("\n");
        sb.append("【状态】 ").append(detail.getStatus()).append("\n");
        sb.append("【耗时】 ").append(detail.getDurationMs()).append("ms\n");
        sb.append("【执行时间】 ").append(detail.getExecutionTime() != null ? detail.getExecutionTime() : "N/A").append("\n");

        if (detail.getExpectedResult() != null && !detail.getExpectedResult().isEmpty()) {
            sb.append("\n【期望结果】\n").append(detail.getExpectedResult()).append("\n");
        }

        if (detail.getActualResult() != null && !detail.getActualResult().isEmpty()) {
            sb.append("\n【实际结果】\n").append(detail.getActualResult()).append("\n");
        }

        if (detail.getFailureReason() != null && !detail.getFailureReason().isEmpty()) {
            sb.append("\n【失败原因】\n").append(detail.getFailureReason()).append("\n");
        }

        if (detail.getOutputLog() != null && !detail.getOutputLog().isEmpty()) {
            sb.append("\n【输出日志】\n").append(detail.getOutputLog()).append("\n");
        }

        if (detail.getStackTrace() != null && !detail.getStackTrace().isEmpty()) {
            sb.append("\n【堆栈跟踪】\n").append(detail.getStackTrace()).append("\n");
        }

        detailArea.setText(sb.toString());
    }

    private HBox createButtonBox() {
        HBox box = new HBox(10);
        box.setPadding(new Insets(8, 10, 8, 10));
        box.setStyle("-fx-background-color: #f5f5f5;");

        exportJsonBtn = new Button("导出JSON");
        exportHtmlBtn = new Button("导出HTML");

        exportJsonBtn.setOnAction(e -> exportReport("json"));
        exportHtmlBtn.setOnAction(e -> exportReport("html"));

        box.getChildren().addAll(exportJsonBtn, exportHtmlBtn);

        return box;
    }

    private Button exportJsonBtn;
    private Button exportHtmlBtn;

    /**
     * 显示报告
     */
    public void showReport(ReportData report) {
        this.currentReport = report;
        updateAutoStats();
        updateManualStats();
        resultTable.getItems().setAll(report.getResults());
        updateExportButtons();

        // 加载手工测试数据（所有批次的详情）
        List<ManualTestBatch> allBatches = manualTestDao.getAllBatches();
        List<ManualTestDetail> allManualDetails = new java.util.ArrayList<>();
        for (ManualTestBatch batch : allBatches) {
            allManualDetails.addAll(manualTestDao.getDetailsByBatchId(batch.getBatchId()));
        }
        manualResultTable.getItems().setAll(allManualDetails);

        // 保存当前报告ID
        config.setLastReportId(report.getReportId());
        configDao.saveConfig(config);
    }

    /**
     * 加载上次报告
     */
    private void loadLastReport() {
        String lastReportId = config.getLastReportId();
        if (lastReportId != null && !lastReportId.isEmpty()) {
            ReportData report = reportDao.loadReport(lastReportId);
            if (report != null) {
                showReport(report);
            }
        }
    }

    private void updateAutoStats() {
        if (currentReport == null) return;

        totalLabel.setText("总脚本数: " + currentReport.getTotalScripts());
        executedLabel.setText("已执行: " + currentReport.getExecutedScripts());
        passedLabel.setText("通过: " + currentReport.getPassedScripts());
        failedLabel.setText("失败: " + currentReport.getFailedScripts());
        blockedLabel.setText("阻塞: " + currentReport.getBlockedScripts());
        timeoutLabel.setText("超时: " + currentReport.getTimeoutScripts());
        skippedLabel.setText("跳过: " + currentReport.getSkippedScripts());
        notExecutedLabel.setText("未执行: " + currentReport.getNotExecutedScripts());
        passRateLabel.setText(String.format("通过率: %.1f%%", currentReport.getPassRate()));

        long duration = (currentReport.getEndTime() - currentReport.getStartTime()) / 1000;
        durationLabel.setText("耗时: " + duration + "s");

        // 显示基线快照
        java.util.Map<String, String> baseline = currentReport.getBaselineSnapshot();
        if (baseline != null && !baseline.isEmpty()) {
            StringBuilder sb = new StringBuilder("基线: ");
            sb.append(baseline.getOrDefault("os.name", "")).append(" | ");
            sb.append("Java ").append(baseline.getOrDefault("java.version", ""));
            String godotMd5 = baseline.get("godot.md5");
            if (godotMd5 != null && !"N/A".equals(godotMd5)) {
                sb.append(" | Godot MD5: ").append(godotMd5);
            }
            baselineLabel.setText(sb.toString());
            baselineLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        } else {
            baselineLabel.setText("");
        }
    }

    private void updateManualStats() {
        List<ManualTestBatch> allBatches = manualTestDao.getAllBatches();
        int total = 0, passed = 0, failed = 0, blocked = 0, pending = 0;

        for (ManualTestBatch batch : allBatches) {
            List<ManualTestDetail> details = manualTestDao.getDetailsByBatchId(batch.getBatchId());
            total += details.size();
            for (ManualTestDetail d : details) {
                switch (d.getStatus()) {
                    case "PASS": passed++; break;
                    case "FAIL": failed++; break;
                    case "BLOCKED": blocked++; break;
                    default: pending++; break;
                }
            }
        }

        int executed = passed + failed + blocked;
        manualTotalLabel.setText("总用例数: " + total);
        manualExecutedLabel.setText("已执行: " + executed);
        manualPassedLabel.setText("通过: " + passed);
        manualFailedLabel.setText("失败: " + failed);
        manualBlockedLabel.setText("阻塞: " + blocked);
        manualPendingLabel.setText("待执行: " + pending);

        double rate = executed > 0 ? (double) passed / executed * 100 : 0;
        manualPassRateLabel.setText(String.format("通过率: %.1f%%", rate));
    }

    private void exportReport(String format) {
        if (currentReport == null) {
            DialogUtil.showError("没有可导出的报告");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出报告");
        fileChooser.setInitialFileName("test_report_" + currentReport.getReportId() + "." + format);

        if ("json".equals(format)) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON文件", "*.json"));
        } else {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML文件", "*.html"));
        }

        File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                if ("json".equals(format)) {
                    exportJson(file);
                } else {
                    exportHtml(file);
                }
                DialogUtil.showInfo("导出成功: " + file.getAbsolutePath());
            } catch (Exception e) {
                DialogUtil.showError("导出失败: " + e.getMessage());
            }
        }
    }

    /**
     * 检查报告中是否包含图片附件
     */
    private boolean hasAttachments() {
        if (currentReport == null || currentReport.getResults() == null) {
            return false;
        }
        for (ExecutionResult result : currentReport.getResults()) {
            if (result.getAttachmentPath() != null && !result.getAttachmentPath().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新导出按钮状态
     */
    private void updateExportButtons() {
        boolean hasImages = hasAttachments();
        exportJsonBtn.setDisable(hasImages);
        if (hasImages) {
            exportJsonBtn.setTooltip(new Tooltip("JSON无法保存图片，请使用HTML导出"));
        } else {
            exportJsonBtn.setTooltip(null);
        }
    }

    private void exportJson(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(file, currentReport);
    }

    private void exportHtml(File file) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<title>测试报告</title>");
        html.append("<style>");
        html.append("body{font-family:Arial,sans-serif;margin:20px;}");
        html.append("table{border-collapse:collapse;width:100%;}");
        html.append("th,td{border:1px solid #ddd;padding:8px;text-align:left;}");
        html.append("th{background-color:#4CAF50;color:white;}");
        html.append(".PASSED{color:green;font-weight:bold;}");
        html.append(".FAILED{color:red;font-weight:bold;}");
        html.append(".BLOCKED{color:orange;font-weight:bold;}");
        html.append(".NOT_EXECUTED{color:gray;}");
        html.append(".stats{margin-bottom:20px;padding:10px;background:#f5f5f5;}");
        html.append(".attachment{margin-top:10px;}");
        html.append(".attachment img{max-width:100%;border:1px solid #ddd;margin:5px 0;}");
        html.append("h2{border-bottom:2px solid #4CAF50;padding-bottom:5px;}");
        html.append("</style></head><body>");

        html.append("<h1>测试报告</h1>");

        // === 自动化测试模块 ===
        html.append("<h2>一、自动化测试</h2>");
        html.append("<div class='stats'>");
        html.append("<p>报告ID: ").append(currentReport.getReportId()).append("</p>");
        html.append("<p>总脚本数: ").append(currentReport.getTotalScripts()).append("</p>");
        html.append("<p>通过: ").append(currentReport.getPassedScripts()).append("</p>");
        html.append("<p>失败: ").append(currentReport.getFailedScripts()).append("</p>");
        html.append("<p>阻塞: ").append(currentReport.getBlockedScripts()).append("</p>");
        html.append("<p>未执行: ").append(currentReport.getNotExecutedScripts()).append("</p>");
        html.append("<p>通过率: ").append(String.format("%.1f%%", currentReport.getPassRate())).append("</p>");
        html.append("</div>");

        html.append("<table><tr><th>脚本名称</th><th>语言</th><th>关联用例</th><th>状态</th><th>优先级</th><th>严重程度</th><th>耗时(ms)</th><th>错误信息</th></tr>");
        for (ExecutionResult result : currentReport.getResults()) {
            html.append("<tr>");
            html.append("<td>").append(escapeHtml(result.getScriptName())).append("</td>");
            html.append("<td>").append(escapeHtml(result.getLanguage())).append("</td>");
            html.append("<td>").append(escapeHtml(result.getTestCasePath() != null ? "已关联" : "未关联")).append("</td>");
            html.append("<td class='").append(result.getStatus()).append("'>").append(result.getStatus()).append("</td>");
            html.append("<td>").append(escapeHtml(result.getPriority() != null ? result.getPriority() : "-")).append("</td>");
            html.append("<td>").append(escapeHtml(result.getSeverity() != null ? result.getSeverity() : "-")).append("</td>");
            html.append("<td>").append(result.getExecutionTime()).append("</td>");
            html.append("<td>").append(escapeHtml(result.getErrorMessage() != null ? result.getErrorMessage() : "")).append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");

        // === 手工测试模块 ===
        html.append("<h2>二、手工测试</h2>");

        // 手工测试统计
        List<ManualTestBatch> allBatches = manualTestDao.getAllBatches();
        int manualTotal = 0, manualPassed = 0, manualFailed = 0, manualBlocked = 0, manualPending = 0;
        List<ManualTestDetail> allManualDetails = new java.util.ArrayList<>();
        for (ManualTestBatch batch : allBatches) {
            List<ManualTestDetail> details = manualTestDao.getDetailsByBatchId(batch.getBatchId());
            allManualDetails.addAll(details);
            manualTotal += details.size();
            for (ManualTestDetail d : details) {
                switch (d.getStatus()) {
                    case "PASS": manualPassed++; break;
                    case "FAIL": manualFailed++; break;
                    case "BLOCKED": manualBlocked++; break;
                    default: manualPending++; break;
                }
            }
        }
        int manualExecuted = manualPassed + manualFailed + manualBlocked;
        double manualRate = manualExecuted > 0 ? (double) manualPassed / manualExecuted * 100 : 0;

        html.append("<div class='stats'>");
        html.append("<p>总用例数: ").append(manualTotal).append("</p>");
        html.append("<p>已执行: ").append(manualExecuted).append("</p>");
        html.append("<p>通过: ").append(manualPassed).append("</p>");
        html.append("<p>失败: ").append(manualFailed).append("</p>");
        html.append("<p>阻塞: ").append(manualBlocked).append("</p>");
        html.append("<p>待执行: ").append(manualPending).append("</p>");
        html.append("<p>通过率: ").append(String.format("%.1f%%", manualRate)).append("</p>");
        html.append("</div>");

        html.append("<table><tr><th>任务名称</th><th>用例ID</th><th>用例名称</th><th>状态</th><th>优先级</th><th>严重程度</th><th>关联自动化用例</th><th>实际结果</th><th>失败原因</th></tr>");
        for (ManualTestDetail detail : allManualDetails) {
            html.append("<tr>");
            // 查找所属任务名称
            String taskName = "";
            for (ManualTestBatch batch : allBatches) {
                if (batch.getBatchId().equals(detail.getBatchId())) {
                    taskName = batch.getTaskName();
                    break;
                }
            }
            html.append("<td>").append(escapeHtml(taskName)).append("</td>");
            html.append("<td>").append(escapeHtml(detail.getCaseId())).append("</td>");
            html.append("<td>").append(escapeHtml(detail.getCaseName())).append("</td>");
            String statusClass = detail.getStatus() != null ? detail.getStatus() : "PENDING";
            html.append("<td class='").append(statusClass).append("'>").append(statusClass).append("</td>");
            html.append("<td>").append(escapeHtml(detail.getPriority() != null ? detail.getPriority() : "-")).append("</td>");
            html.append("<td>").append(escapeHtml(detail.getSeverity() != null ? detail.getSeverity() : "-")).append("</td>");
            html.append("<td>").append(escapeHtml(detail.getLinkedAutoCaseId() != null ? detail.getLinkedAutoCaseId() : "-")).append("</td>");
            html.append("<td>").append(escapeHtml(detail.getActualResult() != null ? detail.getActualResult() : "")).append("</td>");
            html.append("<td>").append(escapeHtml(detail.getFailureReason() != null ? detail.getFailureReason() : "")).append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");

        html.append("</body></html>");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(html.toString());
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}

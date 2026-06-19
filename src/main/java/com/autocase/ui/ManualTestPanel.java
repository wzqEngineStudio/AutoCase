package com.autocase.ui;

import com.autocase.dao.CaseDao;
import com.autocase.dao.ManualTestDao;
import com.autocase.entity.CaseStatus;
import com.autocase.entity.ManualTestBatch;
import com.autocase.entity.ManualTestDetail;
import com.autocase.entity.TaskGroup;
import com.autocase.entity.TestCase;
import com.autocase.logic.CaseLogic;
import com.autocase.util.DialogUtil;
import com.autocase.util.LayoutPersistence;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 手工测试面板 - 树形任务组/任务工作台 + 执行与记录工作区
 *
 * 数据模型: TaskGroup(任务组) → ManualTestBatch(任务) → ManualTestDetail(用例执行记录)
 * FromManager 是系统保留的任务组，该组下只有一个任务(FromManager)，所有从用例管理扫描到的用例都作为该任务的detail记录
 */
public class ManualTestPanel extends BorderPane {

    private final ManualTestDao manualTestDao;
    private final CaseLogic caseLogic;

    // 左侧：树形列表（任务组 → 任务）
    private TreeView<String> taskTreeView;

    // 中间：测试用例清单（选中任务的执行详情）
    private TableView<ManualTestDetail> detailTable;

    // 右侧：执行与记录工作区
    private Label execCaseIdLabel;
    private Label execCaseNameLabel;
    private TextArea stepArea;
    private TextArea expectedArea;
    private TextArea actualResultArea;
    private TextArea failureReasonArea;
    private TextField linkAutoCaseField;
    private ComboBox<String> statusCombo;
    private Label attachmentLabel;
    private String currentAttachmentPath;

    // 当前选中的批次和详情
    private ManualTestBatch currentBatch;
    private ManualTestDetail currentDetail;

    // 运行时树节点数据绑定：用Map存放TreeItem对应的实体（替代getUserData/setUserData）
    private List<TaskGroup> allGroups;
    private List<ManualTestBatch> allBatches;
    private final Map<TreeItem<String>, Object> treeItemDataMap = new HashMap<>();

    public ManualTestPanel() {
        this.manualTestDao = new ManualTestDao();
        this.caseLogic = new CaseLogic();
        buildUI();
        loadHistory();
        ensureFromManagerGroupExists();
    }

    /**
     * 设置根目录并同步FromManager任务组
     * 由 MainWindow 在选择目录或刷新数据时调用
     */
    public void setRootDirectory(String directory) {
        if (directory == null) return;
        System.out.println("[FromManager同步] setRootDirectory 被调用: " + directory);
        caseLogic.loadCases(directory);
        syncFromManagerWhenReady();
    }

    // ==================== FromManager 任务组管理 ====================

    /**
     * 确保 FromManager 任务组存在，并同步用例到该组下的统一任务中
     */
    private void ensureFromManagerGroupExists() {
        TaskGroup fromManagerGroup = manualTestDao.getTaskGroupByName("FromManager");

        if (fromManagerGroup == null) {
            fromManagerGroup = new TaskGroup();
            fromManagerGroup.setGroupName("FromManager");
            fromManagerGroup.setDescription("从用例管理面板自动同步的用例，每个用例一个独立任务");
            fromManagerGroup.setRemark("系统保留任务组");
            fromManagerGroup.setCreateTime(new Timestamp(System.currentTimeMillis()));
            Long gid = manualTestDao.insertTaskGroup(fromManagerGroup);
            fromManagerGroup.setGroupId(gid);
        }

        syncFromManagerCases(fromManagerGroup);
        loadHistory();
    }

    private void syncFromManagerWhenReady() {
        TaskGroup fromManagerGroup = manualTestDao.getTaskGroupByName("FromManager");
        System.out.println("[FromManager同步] syncFromManagerWhenReady: fromManagerGroup=" + (fromManagerGroup != null ? fromManagerGroup.getGroupId() : "null"));
        if (fromManagerGroup != null) {
            syncFromManagerCases(fromManagerGroup);
            loadHistory();
        } else {
            System.out.println("[FromManager同步] FromManager任务组不存在，先创建");
            ensureFromManagerGroupExists();
        }
    }

    /**
     * 同步 FromManager 任务组：FromManager组下只有一个任务(batch)，所有扫描到的用例都作为该任务的detail
     * 同时清理旧模式下产生的碎片任务（每个用例一个batch的残留）
     */
    private void syncFromManagerCases(TaskGroup fromManagerGroup) {
        List<TestCase> allCases = caseLogic.getAllCases();
        System.out.println("[FromManager同步] 扫描到用例数: " + allCases.size());
        if (allCases.isEmpty()) {
            System.out.println("[FromManager同步] 用例列表为空，跳过同步");
            return;
        }

        Long groupId = fromManagerGroup.getGroupId();
        System.out.println("[FromManager同步] FromManager组ID: " + groupId);

        // 获取FromManager组下已有的所有任务
        List<ManualTestBatch> existingBatches = manualTestDao.getBatchesByGroupId(groupId);
        ManualTestBatch fromManagerBatch = null;

        // 优先找名为 "FromManager" 的 batch（统一模式）
        for (ManualTestBatch b : existingBatches) {
            if ("FromManager".equals(b.getTaskName())) {
                fromManagerBatch = b;
                break;
            }
        }

        // 清理旧模式的碎片任务（每个用例一个batch的残留），只保留统一任务
        if (!existingBatches.isEmpty()) {
            for (ManualTestBatch oldBatch : existingBatches) {
                if (oldBatch == fromManagerBatch) continue;  // 跳过统一任务本身
                // 删除旧碎片任务及其关联的detail记录
                System.out.println("[FromManager同步] 清理旧碎片任务: " + oldBatch.getTaskName() + " (batchId=" + oldBatch.getBatchId() + ")");
                manualTestDao.deleteBatch(oldBatch.getBatchId());
            }
        }

        if (fromManagerBatch == null) {
            // 创建唯一的 FromManager 任务
            fromManagerBatch = new ManualTestBatch();
            fromManagerBatch.setGroupId(groupId);
            fromManagerBatch.setTaskName("FromManager");
            fromManagerBatch.setModuleName("从用例管理器获取");
            fromManagerBatch.setTester("系统");
            fromManagerBatch.setTotalCases(0);
            fromManagerBatch.setPassedCount(0);
            fromManagerBatch.setFailedCount(0);
            fromManagerBatch.setBlockedCount(0);
            fromManagerBatch.setPendingCount(0);
            fromManagerBatch.setStartTime(new Timestamp(System.currentTimeMillis()));
            fromManagerBatch.setRemark("从用例管理面板自动同步的用例");

            Long batchId = manualTestDao.insertBatch(fromManagerBatch);
            if (batchId != null) {
                fromManagerBatch.setBatchId(batchId);
                System.out.println("[FromManager同步] 创建统一任务 batchId=" + batchId);
            } else {
                System.err.println("[FromManager同步] 创建统一任务失败！");
                return;
            }
        }

        Long batchId = fromManagerBatch.getBatchId();

        // 获取该任务已有的 detail 记录
        List<ManualTestDetail> existingDetails = manualTestDao.getDetailsByBatchId(batchId);
        java.util.Set<String> existingCaseIds = existingDetails.stream()
                .map(ManualTestDetail::getCaseId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());

        int addedCount = 0;
        for (TestCase tc : allCases) {
            if (!existingCaseIds.contains(tc.getCasesID())) {
                ManualTestDetail detail = new ManualTestDetail();
                detail.setBatchId(batchId);
                detail.setCaseId(tc.getCasesID());
                detail.setCaseName(tc.getCaseName());
                detail.setStatus("PENDING");
                detail.setPriority(tc.getPriority());
                detail.setSeverity(tc.getSeverity());
                if (tc.getExpectedResult() != null && tc.getExpectedResult().getDescription() != null) {
                    detail.setExpectedResult(tc.getExpectedResult().getDescription());
                }
                detail.setExecutionTime(new Timestamp(System.currentTimeMillis()));
                manualTestDao.insertDetail(detail);
                addedCount++;
            }
        }

        // 更新任务的统计数字
        List<ManualTestDetail> allDetails = manualTestDao.getDetailsByBatchId(batchId);
        int pending = 0, passed = 0, failed = 0, blocked = 0;
        for (ManualTestDetail d : allDetails) {
            switch (d.getStatus() != null ? d.getStatus() : "PENDING") {
                case "PASS": passed++; break;
                case "FAIL": failed++; break;
                case "BLOCKED": blocked++; break;
                default: pending++;
            }
        }
        fromManagerBatch.setTotalCases(allDetails.size());
        fromManagerBatch.setPendingCount(pending);
        fromManagerBatch.setPassedCount(passed);
        fromManagerBatch.setFailedCount(failed);
        fromManagerBatch.setBlockedCount(blocked);
        manualTestDao.updateBatch(fromManagerBatch);

        System.out.println("[FromManager同步] 本轮新增用例数: " + addedCount + ", 总用例数: " + allDetails.size());
    }

    // ==================== 公共API ====================

    /**
     * 获取所有非FromManager组的可用任务（供ReportPanel"添加到任务"使用）
     */
    public List<ManualTestBatch> getAvailableTasks() {
        return manualTestDao.getAllBatches().stream()
                .filter(b -> {
                    if (b.getGroupId() == null) return true;
                    TaskGroup g = manualTestDao.getTaskGroupById(b.getGroupId());
                    return g == null || !"FromManager".equals(g.getGroupName());
                })
                .toList();
    }

    public boolean addCaseToTask(Long batchId, TestCase testCase) {
        ManualTestDetail detail = new ManualTestDetail();
        detail.setBatchId(batchId);
        detail.setCaseId(testCase.getCasesID());
        detail.setCaseName(testCase.getCaseName());
        detail.setStatus("PENDING");
        detail.setPriority(testCase.getPriority());
        detail.setSeverity(testCase.getSeverity());
        if (testCase.getExpectedResult() != null && testCase.getExpectedResult().getDescription() != null) {
            detail.setExpectedResult(testCase.getExpectedResult().getDescription());
        }
        detail.setExecutionTime(new Timestamp(System.currentTimeMillis()));
        manualTestDao.insertDetail(detail);

        ManualTestBatch batch = manualTestDao.getAllBatches().stream()
                .filter(b -> b.getBatchId().equals(batchId))
                .findFirst().orElse(null);
        if (batch != null) {
            batch.setTotalCases(batch.getTotalCases() + 1);
            batch.setPendingCount(batch.getPendingCount() + 1);
        }
        loadHistory();
        return true;
    }

    public Long addCaseToTaskReturnId(ManualTestBatch batch, TestCase testCase) {
        Long batchId = manualTestDao.insertBatch(batch);
        if (batchId != null) {
            ManualTestDetail detail = new ManualTestDetail();
            detail.setBatchId(batchId);
            detail.setCaseId(testCase.getCasesID());
            detail.setCaseName(testCase.getCaseName());
            detail.setStatus("PENDING");
            detail.setPriority(testCase.getPriority());
            detail.setSeverity(testCase.getSeverity());
            if (testCase.getExpectedResult() != null && testCase.getExpectedResult().getDescription() != null) {
                detail.setExpectedResult(testCase.getExpectedResult().getDescription());
            }
            detail.setExecutionTime(new Timestamp(System.currentTimeMillis()));
            manualTestDao.insertDetail(detail);
            loadHistory();
        }
        return batchId;
    }

    // ==================== UI 构建 ====================

    private void buildUI() {
        HBox toolBar = createToolBar();

        SplitPane mainSplit = new SplitPane();

        VBox leftPane = createTaskTreePanel();
        VBox centerPane = createCaseListPanel();
        VBox rightPane = createExecutionPanel();

        mainSplit.getItems().addAll(leftPane, centerPane, rightPane);
        double[] manualLayout = LayoutPersistence.getPositions("ManualTestPanel", new double[]{0.22, 0.55});
        mainSplit.setDividerPositions(manualLayout);
        for (SplitPane.Divider divider : mainSplit.getDividers()) {
            divider.positionProperty().addListener((obs, oldVal, newVal) -> {
                LayoutPersistence.savePositions("ManualTestPanel", mainSplit.getDividerPositions());
            });
        }

        setTop(toolBar);
        setCenter(mainSplit);
    }

    private HBox createToolBar() {
        HBox box = new HBox(10);
        box.setPadding(new Insets(8, 10, 8, 10));
        box.setStyle("-fx-background-color: #f5f5f5;");

        Button refreshBtn = new Button("刷新历史");
        Button clearAllBtn = new Button("清除全部历史");

        refreshBtn.setOnAction(e -> loadHistory());
        clearAllBtn.setOnAction(e -> clearAllHistory());

        box.getChildren().addAll(new Region(), refreshBtn, clearAllBtn);
        HBox.setHgrow(new Region(), Priority.ALWAYS);

        return box;
    }

    /**
     * 左栏：树形任务列表（任务组 → 任务）
     */
    private VBox createTaskTreePanel() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(5));

        Label title = new Label("测试任务");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        taskTreeView = new TreeView<>();
        taskTreeView.setShowRoot(false);
        taskTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    TreeItem<String> treeItem = getTreeItem();
                    if (treeItem != null && treeItem.getParent() == taskTreeView.getRoot()) {
                        // 任务组节点（一级）
                        setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                        // 计算子任务数量
                        int childCount = treeItem.getChildren().size();
                        setText(item + " (" + childCount + "个任务)");
                    } else {
                        // 任务节点（二级）- 显示状态颜色
                        Object userData = treeItemDataMap.get(treeItem);
                        if (userData instanceof ManualTestBatch batch) {
                            String statusHint = getStatusHint(batch);
                            setText(item + " " + statusHint);
                            setStatusStyle(batch);
                        }
                    }
                }
            }

            private String getStatusHint(ManualTestBatch batch) {
                int done = (batch.getPassedCount() != null ? batch.getPassedCount() : 0)
                        + (batch.getFailedCount() != null ? batch.getFailedCount() : 0)
                        + (batch.getBlockedCount() != null ? batch.getBlockedCount() : 0);
                int total = batch.getTotalCases() != null ? batch.getTotalCases() : 0;
                return total > 0 ? "[" + done + "/" + total + "]" : "";
            }

            private void setStatusStyle(ManualTestBatch batch) {
                int pending = batch.getPendingCount() != null ? batch.getPendingCount() : 0;
                int failed = batch.getFailedCount() != null ? batch.getFailedCount() : 0;
                int passed = batch.getPassedCount() != null ? batch.getPassedCount() : 0;
                int blocked = batch.getBlockedCount() != null ? batch.getBlockedCount() : 0;
                if (failed > 0) {
                    setStyle("-fx-text-fill: red;");
                } else if (blocked > 0) {
                    setStyle("-fx-text-fill: orange;");
                } else if (pending > 0 && passed == 0) {
                    setStyle("-fx-text-fill: gray;");
                } else if (passed > 0 && pending == 0) {
                    setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });

        // 右键上下文菜单
        ContextMenu contextMenu = new ContextMenu();

        MenuItem addFromCaseItem = new MenuItem("从用例管理添加任务");
        addFromCaseItem.setOnAction(e -> addTaskFromCaseManagement());

        MenuItem newTaskItem = new MenuItem("新建任务");
        newTaskItem.setOnAction(e -> createNewTask());

        MenuItem deleteItem = new MenuItem("删除任务/任务组");
        deleteItem.setOnAction(e -> deleteSelected());

        MenuItem newGroupItem = new MenuItem("新建任务组");
        newGroupItem.setOnAction(e -> createNewTaskGroup());

        MenuItem refreshItem = new MenuItem("刷新");
        refreshItem.setOnAction(e -> loadHistory());

        contextMenu.getItems().addAll(
                addFromCaseItem,
                new SeparatorMenuItem(),
                newTaskItem,
                deleteItem,
                new SeparatorMenuItem(),
                newGroupItem,
                new SeparatorMenuItem(),
                refreshItem
        );
        taskTreeView.setContextMenu(contextMenu);

        // 选中任务时加载用例清单
        taskTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                Object userData = treeItemDataMap.get(newVal);
                if (userData instanceof ManualTestBatch batch) {
                    currentBatch = batch;
                    loadDetails(batch.getBatchId());
                    clearExecutionPanel();
                } else if (userData instanceof TaskGroup group) {
                    // 选中了任务组本身，不加载详情
                    currentBatch = null;
                    detailTable.getItems().clear();
                    clearExecutionPanel();
                }
            }
        });

        box.getChildren().addAll(title, taskTreeView);
        VBox.setVgrow(taskTreeView, Priority.ALWAYS);
        return box;
    }

    /**
     * 加载树形数据
     */
    private void loadHistory() {
        allGroups = manualTestDao.getAllTaskGroups();
        allBatches = manualTestDao.getAllBatches();

        TreeItem<String> rootItem = new TreeItem<>("root");

        for (TaskGroup group : allGroups) {
            TreeItem<String> groupItem = new TreeItem<>(group.getGroupName());
            treeItemDataMap.put(groupItem, group);

            // 查找属于该组的所有任务
            for (ManualTestBatch batch : allBatches) {
                if (group.getGroupId().equals(batch.getGroupId())) {
                    TreeItem<String> batchItem = new TreeItem<>(batch.getTaskName());
                    treeItemDataMap.put(batchItem, batch);
                    groupItem.getChildren().add(batchItem);
                }
            }

            rootItem.getChildren().add(groupItem);
        }

        // 处理没有groupId的旧数据（兼容）
        for (ManualTestBatch batch : allBatches) {
            if (batch.getGroupId() == null) {
                TreeItem<String> orphanItem = new TreeItem<>(batch.getTaskName());
                treeItemDataMap.put(orphanItem, batch);
                rootItem.getChildren().add(orphanItem);
            }
        }

        taskTreeView.setRoot(rootItem);

        // 展开所有节点
        expandAll(taskTreeView.getRoot());

        detailTable.getItems().clear();
        clearExecutionPanel();
    }

    private void expandAll(TreeItem<?> item) {
        if (item == null) return;
        item.setExpanded(true);
        for (TreeItem<?> child : item.getChildren()) {
            expandAll(child);
        }
    }

    // ==================== 中栏：用例清单 ====================

    private VBox createCaseListPanel() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(5));

        Label title = new Label("测试清单");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        detailTable = new TableView<>();
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
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

        TableColumn<ManualTestDetail, String> remarkCol = new TableColumn<>("备注");
        remarkCol.setCellValueFactory(cell -> {
            String r = cell.getValue().getRemark();
            return new javafx.beans.property.SimpleStringProperty(r != null ? r : "");
        });

        detailTable.getColumns().addAll(caseIdCol, caseNameCol, statusCol, remarkCol);

        detailTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentDetail = newVal;
                loadDetailToPanel(newVal);
            }
        });

        box.getChildren().addAll(title, detailTable);
        VBox.setVgrow(detailTable, Priority.ALWAYS);
        return box;
    }

    // ==================== 右栏：执行与记录 ====================

    private VBox createExecutionPanel() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #fafafa;");

        Label title = new Label("执行与记录");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(5);
        infoGrid.setPadding(new Insets(5));

        execCaseIdLabel = new Label("-");
        execCaseNameLabel = new Label("-");
        infoGrid.add(new Label("用例ID:"), 0, 0);
        infoGrid.add(execCaseIdLabel, 1, 0);
        infoGrid.add(new Label("用例名称:"), 0, 1);
        infoGrid.add(execCaseNameLabel, 1, 1);

        stepArea = new TextArea(); stepArea.setEditable(false); stepArea.setWrapText(true); stepArea.setPrefHeight(80);
        expectedArea = new TextArea(); expectedArea.setEditable(false); expectedArea.setWrapText(true); expectedArea.setPrefHeight(50);
        actualResultArea = new TextArea(); actualResultArea.setWrapText(true); actualResultArea.setPrefHeight(60);
        failureReasonArea = new TextArea(); failureReasonArea.setWrapText(true); failureReasonArea.setPrefHeight(50);

        HBox linkAutoBox = new HBox(10);
        linkAutoBox.setAlignment(Pos.CENTER_LEFT);
        linkAutoCaseField = new TextField();
        linkAutoCaseField.setPromptText("输入自动化用例ID进行关联");
        linkAutoCaseField.setMaxWidth(200);
        linkAutoBox.getChildren().addAll(new Label("关联自动化用例:"), linkAutoCaseField);

        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("PENDING", "PASS", "FAIL", "BLOCKED");
        statusCombo.setValue("PENDING");
        statusBox.getChildren().addAll(new Label("执行状态:"), statusCombo);

        HBox attachBox = new HBox(10);
        attachBox.setAlignment(Pos.CENTER_LEFT);
        Button attachBtn = new Button("上传附件(截图)");
        attachmentLabel = new Label("未上传");
        attachBtn.setOnAction(e -> uploadAttachment());
        attachBox.getChildren().addAll(attachBtn, attachmentLabel);

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        Button saveBtn = new Button("保存结果");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        saveBtn.setOnAction(e -> saveResult());
        actionBox.getChildren().addAll(saveBtn);

        box.getChildren().addAll(title, infoGrid,
                new Label("操作步骤/前置条件:"), stepArea,
                new Label("期望结果:"), expectedArea,
                new Label("实际结果:"), actualResultArea,
                new Label("失败原因/备注:"), failureReasonArea,
                linkAutoBox, statusBox, attachBox, actionBox);
        return box;
    }

    private void loadDetails(Long batchId) {
        List<ManualTestDetail> details = manualTestDao.getDetailsByBatchId(batchId);
        detailTable.getItems().setAll(details);
    }

    private void loadDetailToPanel(ManualTestDetail detail) {
        execCaseIdLabel.setText(detail.getCaseId() != null ? detail.getCaseId() : "-");
        execCaseNameLabel.setText(detail.getCaseName() != null ? detail.getCaseName() : "-");

        if (detail.getCaseId() != null) {
            List<TestCase> cases = caseLogic.getAllCases();
            for (TestCase tc : cases) {
                if (detail.getCaseId().equals(tc.getCasesID())) {
                    if (tc.getCaseInfo() != null && tc.getCaseInfo().getStepsToReproduce() != null) {
                        stepArea.setText(String.join("\n", tc.getCaseInfo().getStepsToReproduce()));
                    } else { stepArea.setText(""); }
                    if (tc.getExpectedResult() != null && tc.getExpectedResult().getDescription() != null) {
                        expectedArea.setText(tc.getExpectedResult().getDescription());
                    } else { expectedArea.setText(""); }
                    break;
                }
            }
        }

        statusCombo.setValue(detail.getStatus() != null ? detail.getStatus() : "PENDING");
        actualResultArea.setText(detail.getActualResult() != null ? detail.getActualResult() : "");
        failureReasonArea.setText(detail.getFailureReason() != null ? detail.getFailureReason() : "");
        linkAutoCaseField.setText(detail.getLinkedAutoCaseId() != null ? detail.getLinkedAutoCaseId() : "");

        if (detail.getAttachmentPath() != null && !detail.getAttachmentPath().isEmpty()) {
            attachmentLabel.setText(new File(detail.getAttachmentPath()).getName());
            currentAttachmentPath = detail.getAttachmentPath();
        } else {
            attachmentLabel.setText("未上传");
            currentAttachmentPath = null;
        }
    }

    private void clearExecutionPanel() {
        execCaseIdLabel.setText("-");
        execCaseNameLabel.setText("-");
        stepArea.setText("");
        expectedArea.setText("");
        actualResultArea.setText("");
        failureReasonArea.setText("");
        statusCombo.setValue("PENDING");
        attachmentLabel.setText("未上传");
        currentAttachmentPath = null;
        currentDetail = null;
    }

    private void uploadAttachment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择截图文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(getScene().getWindow());
        if (selectedFile != null) {
            Path attachDir = Paths.get(System.getProperty("user.home"), "cms_attachments");
            try {
                Files.createDirectories(attachDir);
                String destName = System.currentTimeMillis() + "_" + selectedFile.getName();
                Path destPath = attachDir.resolve(destName);
                Files.copy(selectedFile.toPath(), destPath);
                currentAttachmentPath = destPath.toString();
                attachmentLabel.setText(destName);
            } catch (Exception e) {
                DialogUtil.showError("附件保存失败: " + e.getMessage());
            }
        }
    }

    private void saveResult() {
        if (currentDetail == null) {
            DialogUtil.showError("请先选择一条用例记录");
            return;
        }
        currentDetail.setStatus(statusCombo.getValue());
        currentDetail.setActualResult(actualResultArea.getText().trim());
        currentDetail.setFailureReason(failureReasonArea.getText().trim());
        currentDetail.setAttachmentPath(currentAttachmentPath);
        currentDetail.setLinkedAutoCaseId(linkAutoCaseField.getText().trim());
        currentDetail.setExecutionTime(new Timestamp(System.currentTimeMillis()));
        manualTestDao.updateDetail(currentDetail);
        updateBatchStats();
        DialogUtil.showInfo("保存成功");
        loadDetails(currentBatch.getBatchId());
    }

    private void updateBatchStats() {
        if (currentBatch == null) return;
        List<ManualTestDetail> details = manualTestDao.getDetailsByBatchId(currentBatch.getBatchId());
        int passed = 0, failed = 0, blocked = 0, pending = 0;
        for (ManualTestDetail d : details) {
            switch (d.getStatus()) {
                case "PASS": passed++; break;
                case "FAIL": failed++; break;
                case "BLOCKED": blocked++; break;
                default: pending++;
            }
        }
        currentBatch.setPassedCount(passed);
        currentBatch.setFailedCount(failed);
        currentBatch.setBlockedCount(blocked);
        currentBatch.setPendingCount(pending);
        currentBatch.setEndTime(new Timestamp(System.currentTimeMillis()));
    }

    // ==================== 右键菜单功能 ====================

    /**
     * 从用例管理添加任务到选中的任务组
     */
    private void addTaskFromCaseManagement() {
        TreeItem<String> selectedItem = taskTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            DialogUtil.showError("请先选择一个任务组或任务");
            return;
        }

        // 确定目标任务组
        Long targetGroupId = null;
        Object userData = treeItemDataMap.get(selectedItem);
        if (userData instanceof TaskGroup group) {
            targetGroupId = group.getGroupId();
        } else if (userData instanceof ManualTestBatch batch) {
            targetGroupId = batch.getGroupId();
        }

        if (targetGroupId == null) {
            DialogUtil.showError("无法确定目标任务组");
            return;
        }

        showCaseSelectionForGroup(targetGroupId);
    }

    /**
     * 新建任务（在选中的任务组下，或弹窗选择目标组）
     */
    private void createNewTask() {
        TreeItem<String> selectedItem = taskTreeView.getSelectionModel().getSelectedItem();
        Long targetGroupId = null;

        if (selectedItem != null) {
            Object userData = treeItemDataMap.get(selectedItem);
            if (userData instanceof TaskGroup group) {
                targetGroupId = group.getGroupId();
            } else if (userData instanceof ManualTestBatch batch) {
                targetGroupId = batch.getGroupId();
            }
        }

        if (targetGroupId == null) {
            // 让用户选择任务组
            List<TaskGroup> groups = manualTestDao.getAllTaskGroups();
            if (groups.isEmpty()) {
                DialogUtil.showError("暂无任务组，请先创建任务组");
                return;
            }
            ChoiceDialog<TaskGroup> choiceDialog = new ChoiceDialog<>(groups.get(0), groups);
            choiceDialog.setTitle("选择任务组");
            choiceDialog.setHeaderText("请选择要将任务添加到哪个任务组");
            choiceDialog.setContentText("任务组:");
            final Long[] groupIdHolder = new Long[1];
            choiceDialog.showAndWait().ifPresent(g -> groupIdHolder[0] = g.getGroupId());
            targetGroupId = groupIdHolder[0];
        }

        if (targetGroupId == null) return;

        showNewTaskDialog(targetGroupId);
    }

    private void showNewTaskDialog(Long groupId) {
        Dialog<ManualTestBatch> dialog = new Dialog<>();
        dialog.setTitle("新建手工测试任务");
        dialog.setHeaderText("在任务组下创建新任务");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField taskNameField = new TextField();
        TextField versionField = new TextField();
        TextField moduleField = new TextField();
        TextField testerField = new TextField();
        TextArea remarkArea = new TextArea(); remarkArea.setPrefHeight(60);

        grid.add(new Label("任务名称:"), 0, 0); grid.add(taskNameField, 1, 0);
        grid.add(new Label("版本:"), 0, 1); grid.add(versionField, 1, 1);
        grid.add(new Label("功能模块:"), 0, 2); grid.add(moduleField, 1, 2);
        grid.add(new Label("测试人:"), 0, 3); grid.add(testerField, 1, 3);
        grid.add(new Label("备注:"), 0, 4); grid.add(remarkArea, 1, 4);

        dialog.getDialogPane().setContent(grid);
        ButtonType createType = new ButtonType("创建", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == createType) {
                ManualTestBatch batch = new ManualTestBatch();
                batch.setGroupId(groupId);
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
            // 第二步：选择用例
            showCaseSelectionForNewBatch(batch);
        });
    }

    /**
     * 为新任务选择用例
     */
    private void showCaseSelectionForNewBatch(ManualTestBatch batch) {
        Dialog<List<TestCase>> dialog = new Dialog<>();
        dialog.setTitle("选择测试用例");
        dialog.setHeaderText("为新任务 [" + batch.getTaskName() + "] 选择要执行的用例");

        BorderPane content = new BorderPane();
        content.setPadding(new Insets(10));

        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        ComboBox<CaseStatus> statusFilter = new ComboBox<>();
        statusFilter.getItems().add(null);
        statusFilter.getItems().addAll(CaseStatus.values());
        statusFilter.getSelectionModel().select(0);
        statusFilter.setPrefWidth(150);
        Label countLabel = new Label("共 0 条用例");
        countLabel.setStyle("-fx-text-fill: #666;");
        Button selectAllBtn = new Button("全选");
        Button clearAllBtn = new Button("取消全选");
        filterBar.getChildren().addAll(new Label("按状态筛选:"), statusFilter, new Region(), countLabel, selectAllBtn, clearAllBtn);
        HBox.setHgrow(new Region(), Priority.ALWAYS);

        ListView<TestCase> caseListView = new ListView<>();
        caseListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // 修复：JavaFX已知bug - 空列表时点击导致IndexOutOfBoundsException
        // 通过自定义SelectionModel包装，在clearAndSelect前检查边界
        fixListViewEmptyClickBug(caseListView);
        caseListView.setCellFactory(lv -> new ListCell<TestCase>() {
            @Override
            protected void updateItem(TestCase item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    String st = item.getCurrentStatus() != null ? item.getCurrentStatus().getDisplayName() : "未知";
                    setText("[" + st + "] " + item.getCaseName() + " (" + item.getCasesID() + ")");
                    if (item.getCurrentStatus() != null) {
                        switch (item.getCurrentStatus()) {
                            case UNVERIFIED: setStyle("-fx-text-fill: #FF8C00;"); break;
                            case FAILED_NOT_REGRESSED: setStyle("-fx-text-fill: #FF0000;"); break;
                            case PASSED: setStyle("-fx-text-fill: #008000;"); break;
                            case BLOCKED: setStyle("-fx-text-fill: #800080;"); break;
                            default: setStyle("");
                        }
                    }
                }
            }
        });

        List<TestCase> allCases = caseLogic.getAllCases();
        Runnable applyFilter = () -> {
            CaseStatus s = statusFilter.getValue();
            List<TestCase> filtered = (s == null) ? allCases : caseLogic.filterByStatus(s);
            caseListView.getItems().setAll(filtered);
            countLabel.setText("共 " + filtered.size() + " 条用例");
        };
        statusFilter.setOnAction(e -> applyFilter.run());
        applyFilter.run();
        selectAllBtn.setOnAction(e -> caseListView.getSelectionModel().selectAll());
        clearAllBtn.setOnAction(e -> caseListView.getSelectionModel().clearSelection());

        content.setTop(filterBar);
        content.setCenter(caseListView);
        BorderPane.setMargin(filterBar, new Insets(0, 0, 10, 0));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(400);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? caseListView.getSelectionModel().getSelectedItems() : null);

        dialog.showAndWait().ifPresent(selectedCases -> {
            if (selectedCases.isEmpty()) { DialogUtil.showInfo("未选择任何用例"); return; }

            batch.setTotalCases(selectedCases.size());
            batch.setPassedCount(0);
            batch.setFailedCount(0);
            batch.setBlockedCount(0);
            batch.setPendingCount(selectedCases.size());

            Long batchId = manualTestDao.insertBatch(batch);
            if (batchId != null) {
                for (TestCase tc : selectedCases) {
                    ManualTestDetail detail = new ManualTestDetail();
                    detail.setBatchId(batchId);
                    detail.setCaseId(tc.getCasesID());
                    detail.setCaseName(tc.getCaseName());
                    detail.setStatus("PENDING");
                    detail.setPriority(tc.getPriority());
                    detail.setSeverity(tc.getSeverity());
                    if (tc.getExpectedResult() != null && tc.getExpectedResult().getDescription() != null) {
                        detail.setExpectedResult(tc.getExpectedResult().getDescription());
                    }
                    detail.setExecutionTime(new Timestamp(System.currentTimeMillis()));
                    manualTestDao.insertDetail(detail);
                }
                loadHistory();
                DialogUtil.showInfo("任务创建成功，已添加 " + selectedCases.size() + " 个用例");
            }
        });
    }

    /**
     * 向已有任务组添加用例（从用例管理选择）
     */
    private void showCaseSelectionForGroup(Long groupId) {
        Dialog<List<TestCase>> dialog = new Dialog<>();
        TaskGroup group = manualTestDao.getTaskGroupById(groupId);
        dialog.setTitle("从用例管理添加任务 - " + (group != null ? group.getGroupName() : "未知组"));
        dialog.setHeaderText("选择的每个用例将在该任务组下创建为一个独立任务");

        BorderPane content = new BorderPane();
        content.setPadding(new Insets(10));

        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        ComboBox<CaseStatus> statusFilter = new ComboBox<>();
        statusFilter.getItems().add(null);
        statusFilter.getItems().addAll(CaseStatus.values());
        statusFilter.getSelectionModel().select(0);
        statusFilter.setPrefWidth(150);
        Label countLabel = new Label("共 0 条用例");
        countLabel.setStyle("-fx-text-fill: #666;");
        Button selectAllBtn = new Button("全选");
        Button clearAllBtn = new Button("取消全选");
        filterBar.getChildren().addAll(new Label("按状态筛选:"), statusFilter, new Region(), countLabel, selectAllBtn, clearAllBtn);
        HBox.setHgrow(new Region(), Priority.ALWAYS);

        ListView<TestCase> caseListView = new ListView<>();
        caseListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // 修复：JavaFX已知bug - 空列表时点击导致IndexOutOfBoundsException
        fixListViewEmptyClickBug(caseListView);

        // 排除已在组内存在的用例
        List<ManualTestBatch> existingBatches = manualTestDao.getBatchesByGroupId(groupId);
        java.util.Set<String> existingCaseIds = existingBatches.stream()
                .map(ManualTestBatch::getTaskName)
                .collect(java.util.stream.Collectors.toSet());

        caseListView.setCellFactory(lv -> new ListCell<TestCase>() {
            @Override
            protected void updateItem(TestCase item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    String st = item.getCurrentStatus() != null ? item.getCurrentStatus().getDisplayName() : "未知";
                    String prefix = existingCaseIds.contains(item.getCasesID()) ? "[已存在] " : "";
                    setText(prefix + "[" + st + "] " + item.getCaseName() + " (" + item.getCasesID() + ")");
                    if (item.getCurrentStatus() != null) {
                        switch (item.getCurrentStatus()) {
                            case UNVERIFIED: setStyle("-fx-text-fill: #FF8C00;"); break;
                            case FAILED_NOT_REGRESSED: setStyle("-fx-text-fill: #FF0000;"); break;
                            case PASSED: setStyle("-fx-text-fill: #008000;"); break;
                            case BLOCKED: setStyle("-fx-text-fill: #800080;"); break;
                            default: setStyle("");
                        }
                    }
                    if (existingCaseIds.contains(item.getCasesID())) {
                        setStyle("-fx-text-fill: #999999; -fx-font-style: italic;");
                        setDisable(true);
                    }
                }
            }
        });

        List<TestCase> allCases = caseLogic.getAllCases();
        Runnable applyFilter = () -> {
            CaseStatus s = statusFilter.getValue();
            List<TestCase> filtered = (s == null) ? allCases : caseLogic.filterByStatus(s);
            caseListView.getItems().setAll(filtered);
            countLabel.setText("共 " + filtered.size() + " 条用例");
        };
        statusFilter.setOnAction(e -> applyFilter.run());
        applyFilter.run();
        selectAllBtn.setOnAction(e -> caseListView.getSelectionModel().selectAll());
        clearAllBtn.setOnAction(e -> caseListView.getSelectionModel().clearSelection());

        content.setTop(filterBar);
        content.setCenter(caseListView);
        BorderPane.setMargin(filterBar, new Insets(0, 0, 10, 0));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.getDialogPane().setPrefHeight(400);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? caseListView.getSelectionModel().getSelectedItems() : null);

        dialog.showAndWait().ifPresent(selectedCases -> {
            List<TestCase> newCases = selectedCases.stream()
                    .filter(tc -> !existingCaseIds.contains(tc.getCasesID()))
                    .toList();
            if (newCases.isEmpty()) { DialogUtil.showInfo("没有新的用例需要添加"); return; }

            int addedCount = 0;
            for (TestCase tc : newCases) {
                ManualTestBatch batch = new ManualTestBatch();
                batch.setGroupId(groupId);
                batch.setTaskName(tc.getCasesID());
                batch.setModuleName(group != null ? group.getGroupName() : "");
                batch.setTester("");
                batch.setTotalCases(1);
                batch.setPendingCount(1);
                batch.setStartTime(new Timestamp(System.currentTimeMillis()));

                Long batchId = manualTestDao.insertBatch(batch);
                if (batchId != null) {
                    ManualTestDetail detail = new ManualTestDetail();
                    detail.setBatchId(batchId);
                    detail.setCaseId(tc.getCasesID());
                    detail.setCaseName(tc.getCaseName());
                    detail.setStatus("PENDING");
                    detail.setPriority(tc.getPriority());
                    detail.setSeverity(tc.getSeverity());
                    if (tc.getExpectedResult() != null && tc.getExpectedResult().getDescription() != null) {
                        detail.setExpectedResult(tc.getExpectedResult().getDescription());
                    }
                    detail.setExecutionTime(new Timestamp(System.currentTimeMillis()));
                    manualTestDao.insertDetail(detail);
                    addedCount++;
                }
            }
            loadHistory();
            DialogUtil.showInfo("成功添加 " + addedCount + " 个任务到任务组");
        });
    }

    /**
     * 新建任务组
     */
    private void createNewTaskGroup() {
        Dialog<TaskGroup> dialog = new Dialog<>();
        dialog.setTitle("新建任务组");
        dialog.setHeaderText("创建一个新的任务组，用于组织相关的测试任务");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        TextField descField = new TextField();
        TextArea remarkArea = new TextArea(); remarkArea.setPrefHeight(60);

        grid.add(new Label("任务组名称:*"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("描述:"), 0, 1); grid.add(descField, 1, 1);
        grid.add(new Label("备注:"), 0, 2); grid.add(remarkArea, 1, 2);

        dialog.getDialogPane().setContent(grid);
        ButtonType createType = new ButtonType("创建", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == createType) {
                if (nameField.getText().isBlank()) return null;
                TaskGroup g = new TaskGroup();
                g.setGroupName(nameField.getText().trim());
                g.setDescription(descField.getText().trim());
                g.setRemark(remarkArea.getText().trim());
                g.setCreateTime(new Timestamp(System.currentTimeMillis()));
                return g;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(group -> {
            Long gid = manualTestDao.insertTaskGroup(group);
            if (gid != null) {
                loadHistory();
                DialogUtil.showInfo("任务组已创建: " + group.getGroupName());
            }
        });
    }

    /**
     * 删除选中的任务或任务组
     */
    private void deleteSelected() {
        TreeItem<String> selectedItem = taskTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            DialogUtil.showError("请先选择一个任务或任务组");
            return;
        }

        Object userData = treeItemDataMap.get(selectedItem);
        if (userData instanceof TaskGroup group) {
            if ("FromManager".equals(group.getGroupName())) {
                DialogUtil.showError("FromManager 为系统保留任务组，不允许删除");
                return;
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除任务组");
            alert.setHeaderText(null);
            alert.setContentText("确定要删除任务组 \"" + group.getGroupName() + "\" 吗？\n该组下的所有任务和用例记录将被一并删除，此操作不可恢复。");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    manualTestDao.deleteTaskGroup(group.getGroupId());
                    loadHistory();
                    DialogUtil.showInfo("任务组已删除: " + group.getGroupName());
                }
            });
        } else if (userData instanceof ManualTestBatch batch) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除任务");
            alert.setHeaderText(null);
            alert.setContentText("确定要删除任务 \"" + batch.getTaskName() + "\" 吗？\n该任务下的所有用例记录将被一并删除，此操作不可恢复。");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    manualTestDao.deleteBatch(batch.getBatchId());
                    loadHistory();
                    DialogUtil.showInfo("任务已删除: " + batch.getTaskName());
                }
            });
        }
    }

    private void clearAllHistory() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认清除");
        alert.setHeaderText(null);
        alert.setContentText("确定要清除所有手工测试历史吗？此操作不可恢复。");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                manualTestDao.clearAllHistory();
                ensureFromManagerGroupExists();
                DialogUtil.showInfo("已清除所有手工测试历史");
            }
        });
    }

    /**
     * 修复JavaFX ListView已知bug：空列表时点击导致IndexOutOfBoundsException
     * 原因：ListViewBehavior在鼠标按下时调用clearAndSelect(0)，但空列表size=0导致越界
     * 方案：委托给DialogUtil统一处理，当列表为空时禁用ListView
     */
    private static <T> void fixListViewEmptyClickBug(ListView<T> listView) {
        DialogUtil.fixListViewEmptyClickBug(listView);
    }
}

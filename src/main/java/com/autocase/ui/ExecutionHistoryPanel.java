package com.autocase.ui;

import com.autocase.dao.ExecutionHistoryDao;
import com.autocase.entity.ExecutionBatch;
import com.autocase.entity.ExecutionDetail;
import com.autocase.util.DialogUtil;
import com.autocase.util.LayoutPersistence;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 执行历史面板 - 显示 H2 数据库中的执行记录
 */
public class ExecutionHistoryPanel extends BorderPane {

    private final ExecutionHistoryDao historyDao;
    private TableView<ExecutionBatch> batchTable;
    private TableView<ExecutionDetail> detailTable;
    private TextArea detailArea;

    public ExecutionHistoryPanel() {
        this.historyDao = new ExecutionHistoryDao();
        buildUI();
        loadHistory();
    }

    private void buildUI() {
        // 批次表格
        batchTable = new TableView<>();
        batchTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ExecutionBatch, Long> batchIdCol = new TableColumn<>("批次ID");
        batchIdCol.setCellValueFactory(new PropertyValueFactory<>("batchId"));
        batchIdCol.setPrefWidth(80);

        TableColumn<ExecutionBatch, String> triggerCol = new TableColumn<>("触发方式");
        triggerCol.setCellValueFactory(new PropertyValueFactory<>("triggerType"));
        triggerCol.setPrefWidth(80);

        TableColumn<ExecutionBatch, String> userCol = new TableColumn<>("触发者");
        userCol.setCellValueFactory(new PropertyValueFactory<>("triggerUser"));
        userCol.setPrefWidth(80);

        TableColumn<ExecutionBatch, Integer> totalCol = new TableColumn<>("总数");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalCases"));
        totalCol.setPrefWidth(50);

        TableColumn<ExecutionBatch, Integer> passedCol = new TableColumn<>("通过");
        passedCol.setCellValueFactory(new PropertyValueFactory<>("passedCount"));
        passedCol.setPrefWidth(50);

        TableColumn<ExecutionBatch, Integer> failedCol = new TableColumn<>("失败");
        failedCol.setCellValueFactory(new PropertyValueFactory<>("failedCount"));
        failedCol.setPrefWidth(50);

        TableColumn<ExecutionBatch, String> timeCol = new TableColumn<>("开始时间");
        timeCol.setCellValueFactory(cell -> {
            if (cell.getValue().getStartTime() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    cell.getValue().getStartTime().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        timeCol.setPrefWidth(140);

        TableColumn<ExecutionBatch, String> durationCol = new TableColumn<>("耗时");
        durationCol.setCellValueFactory(cell -> {
            if (cell.getValue().getStartTime() != null && cell.getValue().getEndTime() != null) {
                long ms = cell.getValue().getEndTime().getTime() - cell.getValue().getStartTime().getTime();
                return new javafx.beans.property.SimpleStringProperty(formatDuration(ms));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        durationCol.setPrefWidth(80);

        batchTable.getColumns().addAll(batchIdCol, triggerCol, userCol, totalCol, passedCol, failedCol, timeCol, durationCol);

        // 详情表格
        detailTable = new TableView<>();
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ExecutionDetail, String> caseIdCol = new TableColumn<>("用例ID");
        caseIdCol.setCellValueFactory(new PropertyValueFactory<>("caseId"));
        caseIdCol.setPrefWidth(120);

        TableColumn<ExecutionDetail, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(80);

        TableColumn<ExecutionDetail, Integer> durationMsCol = new TableColumn<>("耗时(ms)");
        durationMsCol.setCellValueFactory(new PropertyValueFactory<>("durationMs"));
        durationMsCol.setPrefWidth(80);

        TableColumn<ExecutionDetail, String> reasonCol = new TableColumn<>("失败原因");
        reasonCol.setCellValueFactory(cell -> {
            String reason = cell.getValue().getFailureReason();
            return new javafx.beans.property.SimpleStringProperty(reason != null ? reason : "");
        });
        reasonCol.setPrefWidth(200);

        detailTable.getColumns().addAll(caseIdCol, statusCol, durationMsCol, reasonCol);

        // 批次选中时加载详情
        batchTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadDetails(newVal.getBatchId());
            }
        });

        // 详情选中时显示输出日志
        detailTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String log = newVal.getOutputLog();
                String reason = newVal.getFailureReason();
                StringBuilder sb = new StringBuilder();
                if (reason != null && !reason.isEmpty()) {
                    sb.append("【失败原因】\n").append(reason).append("\n\n");
                }
                if (log != null && !log.isEmpty()) {
                    sb.append("【输出日志】\n").append(log);
                }
                detailArea.setText(sb.toString());
            } else {
                detailArea.setText("选择一条记录查看详情");
            }
        });

        // 工具栏
        HBox toolBar = createToolBar();

        // 详情区域 - 使用 TextArea 支持 Ctrl+A 全选
        detailArea = new TextArea("选择一条记录查看详情");
        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setPrefHeight(150);
        ScrollPane detailScroll = new ScrollPane(detailArea);
        detailScroll.setFitToWidth(true);
        detailScroll.setPrefHeight(150);

        // 布局
        VBox leftPane = new VBox(5, new Label("执行批次"), batchTable);
        VBox.setVgrow(batchTable, Priority.ALWAYS);

        VBox rightPane = new VBox(5, new Label("执行详情"), detailTable, new Label("详情输出"), detailScroll);
        VBox.setVgrow(detailTable, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(leftPane, rightPane);
        double[] historyLayout = LayoutPersistence.getPositions("ExecutionHistoryPanel", new double[]{0.4});
        splitPane.setDividerPositions(historyLayout);
        splitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            LayoutPersistence.savePositions("ExecutionHistoryPanel", splitPane.getDividerPositions());
        });

        setTop(toolBar);
        setCenter(splitPane);
    }

    private HBox createToolBar() {
        HBox box = new HBox(10);
        box.setPadding(new Insets(8, 10, 8, 10));
        box.setStyle("-fx-background-color: #f5f5f5;");

        Button refreshBtn = new Button("刷新历史");
        Button clearAllBtn = new Button("清除全部历史");
        Button cleanBtn = new Button("清理过期数据");
        Button deleteBtn = new Button("删除选中批次");

        refreshBtn.setOnAction(e -> loadHistory());
        clearAllBtn.setOnAction(e -> clearAllHistory());
        cleanBtn.setOnAction(e -> cleanOldData());
        deleteBtn.setOnAction(e -> deleteSelectedBatch());

        box.getChildren().addAll(refreshBtn, deleteBtn, new Region(), cleanBtn, clearAllBtn);
        HBox.setHgrow(new Region(), Priority.ALWAYS);

        return box;
    }

    private void loadHistory() {
        List<ExecutionBatch> batches = historyDao.getAllBatches();
        batchTable.getItems().setAll(batches);
        detailTable.getItems().clear();
        detailArea.setText("选择一条记录查看详情");
    }

    private void loadDetails(Long batchId) {
        List<ExecutionDetail> details = historyDao.getDetailsByBatchId(batchId);
        detailTable.getItems().setAll(details);
    }

    private void cleanOldData() {
        TextInputDialog dialog = new TextInputDialog("30");
        dialog.setTitle("清理过期数据");
        dialog.setHeaderText(null);
        dialog.setContentText("保留最近多少天的数据？");

        dialog.showAndWait().ifPresent(days -> {
            try {
                int d = Integer.parseInt(days);
                historyDao.cleanOldData(d);
                loadHistory();
                DialogUtil.showInfo("已清理 " + d + " 天前的数据");
            } catch (NumberFormatException e) {
                DialogUtil.showError("请输入有效数字");
            }
        });
    }

    private void deleteSelectedBatch() {
        ExecutionBatch selected = batchTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showError("请先选择要删除的批次");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除批次 #" + selected.getBatchId() + " 及其所有详情吗？");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                historyDao.deleteBatch(selected.getBatchId());
                loadHistory();
                DialogUtil.showInfo("已删除批次 #" + selected.getBatchId());
            }
        });
    }

    private void clearAllHistory() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认清除");
        alert.setHeaderText(null);
        alert.setContentText("确定要清除所有执行历史吗？此操作不可恢复。");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                historyDao.clearAllHistory();
                loadHistory();
                DialogUtil.showInfo("已清除所有执行历史");
            }
        });
    }

    private String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return (ms / 1000) + "s";
        return (ms / 60000) + "m" + ((ms % 60000) / 1000) + "s";
    }
}

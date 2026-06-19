package com.autocase.ui;

import com.autocase.dao.DefectDao;
import com.autocase.entity.Defect;
import com.autocase.entity.TestCase;
import com.autocase.logic.CaseLogic;
import com.autocase.util.DialogUtil;
import com.autocase.util.LayoutPersistence;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 缺陷管理面板 - 记录和管理手工/自动化测试发现的缺陷
 */
public class DefectPanel extends BorderPane {

    private final DefectDao defectDao;
    private final CaseLogic caseLogic;

    private TableView<Defect> defectTable;
    private TextArea detailArea;

    public DefectPanel() {
        this.defectDao = new DefectDao();
        this.caseLogic = new CaseLogic();
        buildUI();
        loadDefects();
    }

    private void buildUI() {
        // 工具栏
        HBox toolBar = createToolBar();

        // 主内容区：上下布局
        SplitPane mainSplit = new SplitPane();

        // 上：缺陷列表
        VBox listPane = createDefectListPanel();

        // 下：缺陷详情
        VBox detailPane = createDetailPanel();

        mainSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        mainSplit.getItems().addAll(listPane, detailPane);
        double[] defectLayout = LayoutPersistence.getPositions("DefectPanel", new double[]{0.6});
        mainSplit.setDividerPositions(defectLayout);
        mainSplit.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            LayoutPersistence.savePositions("DefectPanel", mainSplit.getDividerPositions());
        });

        setTop(toolBar);
        setCenter(mainSplit);
    }

    private HBox createToolBar() {
        HBox box = new HBox(10);
        box.setPadding(new Insets(8, 10, 8, 10));
        box.setStyle("-fx-background-color: #f5f5f5;");

        Button newDefectBtn = new Button("新建缺陷");
        Button refreshBtn = new Button("刷新");
        Button clearAllBtn = new Button("清除全部");

        newDefectBtn.setOnAction(e -> createNewDefect());
        refreshBtn.setOnAction(e -> loadDefects());
        clearAllBtn.setOnAction(e -> clearAllDefects());

        box.getChildren().addAll(newDefectBtn, new Region(), refreshBtn, clearAllBtn);
        HBox.setHgrow(new Region(), Priority.ALWAYS);

        return box;
    }

    private VBox createDefectListPanel() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(5));

        Label title = new Label("缺陷列表");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        defectTable = new TableView<>();
        defectTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Defect, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("defectId"));
        idCol.setPrefWidth(50);

        TableColumn<Defect, String> titleCol = new TableColumn<>("缺陷标题");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("defectTitle"));
        titleCol.setPrefWidth(200);

        TableColumn<Defect, String> caseIdCol = new TableColumn<>("用例ID");
        caseIdCol.setCellValueFactory(new PropertyValueFactory<>("caseId"));
        caseIdCol.setPrefWidth(100);

        TableColumn<Defect, String> sourceCol = new TableColumn<>("来源");
        sourceCol.setCellValueFactory(new PropertyValueFactory<>("source"));
        sourceCol.setPrefWidth(80);
        sourceCol.setCellFactory(col -> new TableCell<Defect, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("MANUAL".equals(item)) {
                        setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: purple; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<Defect, String> severityCol = new TableColumn<>("严重程度");
        severityCol.setCellValueFactory(new PropertyValueFactory<>("severity"));
        severityCol.setPrefWidth(80);
        severityCol.setCellFactory(col -> new TableCell<Defect, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "CRITICAL": setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); break;
                        case "MAJOR": setStyle("-fx-text-fill: orange; -fx-font-weight: bold;"); break;
                        case "MINOR": setStyle("-fx-text-fill: #FFA500;"); break;
                        case "TRIVIAL": setStyle("-fx-text-fill: gray;"); break;
                        default: setStyle("");
                    }
                }
            }
        });

        TableColumn<Defect, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(70);
        statusCol.setCellFactory(col -> new TableCell<Defect, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "OPEN": setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); break;
                        case "FIXED": setStyle("-fx-text-fill: green; -fx-font-weight: bold;"); break;
                        case "CLOSED": setStyle("-fx-text-fill: gray;"); break;
                        case "REOPENED": setStyle("-fx-text-fill: orange; -fx-font-weight: bold;"); break;
                        default: setStyle("");
                    }
                }
            }
        });

        TableColumn<Defect, String> reporterCol = new TableColumn<>("报告人");
        reporterCol.setCellValueFactory(new PropertyValueFactory<>("reporter"));
        reporterCol.setPrefWidth(80);

        TableColumn<Defect, String> timeCol = new TableColumn<>("创建时间");
        timeCol.setCellValueFactory(cell -> {
            if (cell.getValue().getCreatedTime() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().getCreatedTime().toLocalDateTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        timeCol.setPrefWidth(100);

        defectTable.getColumns().addAll(idCol, titleCol, caseIdCol, sourceCol, severityCol, statusCol, reporterCol, timeCol);

        // 选中缺陷时显示详情
        defectTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showDefectDetail(newVal);
            }
        });

        box.getChildren().addAll(title, defectTable);
        VBox.setVgrow(defectTable, Priority.ALWAYS);
        return box;
    }

    private VBox createDetailPanel() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #fafafa;");

        Label title = new Label("缺陷详情");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        detailArea = new TextArea();
        detailArea.setEditable(false);
        detailArea.setWrapText(true);

        // 操作按钮
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button fixBtn = new Button("标记为已修复");
        Button closeBtn = new Button("标记为已关闭");
        Button reopenBtn = new Button("重新打开");
        Button deleteBtn = new Button("删除缺陷");

        fixBtn.setOnAction(e -> changeStatus("FIXED"));
        closeBtn.setOnAction(e -> changeStatus("CLOSED"));
        reopenBtn.setOnAction(e -> changeStatus("REOPENED"));
        deleteBtn.setOnAction(e -> deleteSelectedDefect());

        actionBox.getChildren().addAll(fixBtn, closeBtn, reopenBtn, deleteBtn);

        box.getChildren().addAll(title, detailArea, actionBox);
        return box;
    }

    private void loadDefects() {
        List<Defect> defects = defectDao.getAllDefects();
        defectTable.getItems().setAll(defects);
        detailArea.setText("选择一条缺陷查看详情");
    }

    private void showDefectDetail(Defect defect) {
        StringBuilder sb = new StringBuilder();
        sb.append("【缺陷ID】 ").append(defect.getDefectId()).append("\n");
        sb.append("【缺陷标题】 ").append(defect.getDefectTitle()).append("\n");
        sb.append("【用例ID】 ").append(defect.getCaseId() != null ? defect.getCaseId() : "N/A").append("\n");
        sb.append("【用例名称】 ").append(defect.getCaseName() != null ? defect.getCaseName() : "N/A").append("\n");
        sb.append("【来源】 ").append("MANUAL".equals(defect.getSource()) ? "手工测试" : "自动化测试").append("\n");
        sb.append("【严重程度】 ").append(defect.getSeverity()).append("\n");
        sb.append("【状态】 ").append(defect.getStatus()).append("\n");
        sb.append("【报告人】 ").append(defect.getReporter() != null ? defect.getReporter() : "N/A").append("\n");
        sb.append("【指派给】 ").append(defect.getAssignee() != null ? defect.getAssignee() : "N/A").append("\n");
        sb.append("【创建时间】 ").append(defect.getCreatedTime() != null ? defect.getCreatedTime() : "N/A").append("\n");
        sb.append("【更新时间】 ").append(defect.getUpdatedTime() != null ? defect.getUpdatedTime() : "N/A").append("\n");

        if (defect.getDescription() != null && !defect.getDescription().isEmpty()) {
            sb.append("\n【缺陷描述】\n").append(defect.getDescription()).append("\n");
        }

        if (defect.getSteps() != null && !defect.getSteps().isEmpty()) {
            sb.append("\n【操作步骤】\n").append(defect.getSteps()).append("\n");
        }

        if (defect.getExpectedResult() != null && !defect.getExpectedResult().isEmpty()) {
            sb.append("\n【期望结果】\n").append(defect.getExpectedResult()).append("\n");
        }

        if (defect.getActualResult() != null && !defect.getActualResult().isEmpty()) {
            sb.append("\n【实际结果】\n").append(defect.getActualResult()).append("\n");
        }

        if (defect.getAttachmentPath() != null && !defect.getAttachmentPath().isEmpty()) {
            sb.append("\n【附件路径】 ").append(defect.getAttachmentPath()).append("\n");
        }

        if (defect.getRemark() != null && !defect.getRemark().isEmpty()) {
            sb.append("\n【备注】\n").append(defect.getRemark()).append("\n");
        }

        detailArea.setText(sb.toString());
    }

    private void changeStatus(String newStatus) {
        Defect selected = defectTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showError("请先选择一条缺陷记录");
            return;
        }

        selected.setStatus(newStatus);
        selected.setUpdatedTime(new Timestamp(System.currentTimeMillis()));
        defectDao.updateDefect(selected);

        DialogUtil.showInfo("缺陷状态已更新为: " + newStatus);
        loadDefects();
    }

    private void deleteSelectedDefect() {
        Defect selected = defectTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showError("请先选择一条缺陷记录");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除该缺陷吗？此操作不可恢复。");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                defectDao.deleteDefect(selected.getDefectId());
                DialogUtil.showInfo("缺陷已删除");
                loadDefects();
            }
        });
    }

    private void createNewDefect() {
        Dialog<Defect> dialog = new Dialog<>();
        dialog.setTitle("新建缺陷");
        dialog.setHeaderText("请输入缺陷信息");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        TextField caseIdField = new TextField();
        ComboBox<String> sourceCombo = new ComboBox<>();
        sourceCombo.getItems().addAll("MANUAL", "AUTO");
        sourceCombo.setValue("MANUAL");

        ComboBox<String> severityCombo = new ComboBox<>();
        severityCombo.getItems().addAll("CRITICAL", "MAJOR", "MINOR", "TRIVIAL");
        severityCombo.setValue("MAJOR");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("OPEN", "FIXED", "CLOSED", "REOPENED");
        statusCombo.setValue("OPEN");

        TextField reporterField = new TextField();
        TextField assigneeField = new TextField();
        TextArea descArea = new TextArea();
        descArea.setPrefHeight(60);
        TextArea stepsArea = new TextArea();
        stepsArea.setPrefHeight(60);
        TextArea expectedArea = new TextArea();
        expectedArea.setPrefHeight(40);
        TextArea actualArea = new TextArea();
        actualArea.setPrefHeight(40);

        grid.add(new Label("缺陷标题:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("用例ID:"), 0, 1);
        grid.add(caseIdField, 1, 1);
        grid.add(new Label("来源:"), 0, 2);
        grid.add(sourceCombo, 1, 2);
        grid.add(new Label("严重程度:"), 0, 3);
        grid.add(severityCombo, 1, 3);
        grid.add(new Label("状态:"), 0, 4);
        grid.add(statusCombo, 1, 4);
        grid.add(new Label("报告人:"), 0, 5);
        grid.add(reporterField, 1, 5);
        grid.add(new Label("指派给:"), 0, 6);
        grid.add(assigneeField, 1, 6);
        grid.add(new Label("缺陷描述:"), 0, 7);
        grid.add(descArea, 1, 7);
        grid.add(new Label("操作步骤:"), 0, 8);
        grid.add(stepsArea, 1, 8);
        grid.add(new Label("期望结果:"), 0, 9);
        grid.add(expectedArea, 1, 9);
        grid.add(new Label("实际结果:"), 0, 10);
        grid.add(actualArea, 1, 10);

        dialog.getDialogPane().setContent(grid);

        ButtonType createButtonType = new ButtonType("创建", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == createButtonType) {
                Defect defect = new Defect();
                defect.setDefectTitle(titleField.getText().trim());
                defect.setCaseId(caseIdField.getText().trim());

                // 根据用例ID获取用例名称
                String caseId = caseIdField.getText().trim();
                if (!caseId.isEmpty()) {
                    List<TestCase> cases = caseLogic.getAllCases();
                    for (TestCase tc : cases) {
                        if (caseId.equals(tc.getCasesID())) {
                            defect.setCaseName(tc.getCaseName());
                            break;
                        }
                    }
                }

                defect.setSource(sourceCombo.getValue());
                defect.setSeverity(severityCombo.getValue());
                defect.setStatus(statusCombo.getValue());
                defect.setReporter(reporterField.getText().trim());
                defect.setAssignee(assigneeField.getText().trim());
                defect.setDescription(descArea.getText().trim());
                defect.setSteps(stepsArea.getText().trim());
                defect.setExpectedResult(expectedArea.getText().trim());
                defect.setActualResult(actualArea.getText().trim());
                defect.setCreatedTime(new Timestamp(System.currentTimeMillis()));
                defect.setUpdatedTime(new Timestamp(System.currentTimeMillis()));
                return defect;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(defect -> {
            Long id = defectDao.insertDefect(defect);
            if (id != null) {
                DialogUtil.showInfo("缺陷创建成功");
                loadDefects();
            }
        });
    }

    private void clearAllDefects() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认清除");
        alert.setHeaderText(null);
        alert.setContentText("确定要清除所有缺陷记录吗？此操作不可恢复。");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                defectDao.clearAllDefects();
                DialogUtil.showInfo("已清除所有缺陷记录");
                loadDefects();
            }
        });
    }
}

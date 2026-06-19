package com.autocase.ui;

import com.autocase.entity.CaseStatus;
import com.autocase.entity.ManualTestBatch;
import com.autocase.entity.Severity;
import com.autocase.entity.TestCase;
import com.autocase.logic.CaseLogic;
import com.autocase.logic.CaseTemplateGenerator;
import com.autocase.util.DialogUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;

import java.util.List;
import java.util.function.Consumer;

/**
 * 用例筛选面板 - 提供多种筛选条件和用例列表展示
 */
public class CaseFilterPanel extends VBox {

    private final CaseLogic caseLogic;
    private final TableView<TestCase> caseTable;
    private final ObservableList<TestCase> tableData;

    // 筛选类型选择
    private final ComboBox<FilterType> filterTypeCombo;

    // 筛选控件
    private final TextField keywordField;
    private final TextField idStartField;
    private final TextField idEndField;
    private final ComboBox<CaseStatus> statusCombo;
    private final ComboBox<String> severityCombo;
    private final ComboBox<String> priorityCombo;

    public CaseFilterPanel(CaseLogic caseLogic) {
        this.caseLogic = caseLogic;
        this.tableData = FXCollections.observableArrayList();

        // 筛选类型
        filterTypeCombo = new ComboBox<>();
        filterTypeCombo.getItems().addAll(FilterType.values());
        filterTypeCombo.setValue(FilterType.KEYWORD);

        // 初始化筛选控件
        keywordField = new TextField();
        keywordField.setPromptText("输入关键字");

        idStartField = new TextField();
        idStartField.setPromptText("起始编号");
        idStartField.setPrefWidth(80);

        idEndField = new TextField();
        idEndField.setPromptText("结束编号");
        idEndField.setPrefWidth(80);

        statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(CaseStatus.values());
        statusCombo.setPromptText("选择状态");

        severityCombo = new ComboBox<>();
        severityCombo.setPromptText("选择严重程度");

        priorityCombo = new ComboBox<>();
        priorityCombo.setPromptText("选择优先级");

        // 初始化表格
        caseTable = createCaseTable();

        // 构建UI
        buildUI();

        // 监听筛选类型变化
        filterTypeCombo.setOnAction(e -> updateFilterControls());
        updateFilterControls();
    }

    /**
     * 筛选类型枚举
     */
    public enum FilterType {
        KEYWORD("关键字"),
        ID_RANGE("编号范围"),
        STATUS("状态"),
        SEVERITY("严重程度"),
        PRIORITY("优先级");

        private final String displayName;

        FilterType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * 构建UI布局
     */
    private void buildUI() {
        setSpacing(10);
        setPadding(new Insets(10));

        // 筛选条件区域
        HBox filterBox = new HBox(10);
        filterBox.setPadding(new Insets(5));
        filterBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label typeLabel = new Label("筛选类型:");

        Button filterButton = new Button("筛选");
        Button resetButton = new Button("重置");
        Button templateBtn = new Button("查看用例模板");

        // 筛选值容器（动态切换）
        HBox valueBox = new HBox(5);
        valueBox.getChildren().addAll(keywordField, idStartField, idEndField, statusCombo, severityCombo, priorityCombo);
        HBox.setHgrow(valueBox, Priority.ALWAYS);

        filterBox.getChildren().addAll(typeLabel, filterTypeCombo, valueBox, filterButton, resetButton, templateBtn);

        // 按钮事件
        filterButton.setOnAction(e -> applyFilter());
        resetButton.setOnAction(e -> resetFilter());
        templateBtn.setOnAction(e -> showTemplateDialog());

        getChildren().addAll(filterBox, caseTable);
    }

    /**
     * 根据筛选类型更新控件可见性
     */
    private void updateFilterControls() {
        FilterType type = filterTypeCombo.getValue();
        boolean showKeyword = type == FilterType.KEYWORD;
        boolean showIdRange = type == FilterType.ID_RANGE;
        boolean showStatus = type == FilterType.STATUS;
        boolean showSeverity = type == FilterType.SEVERITY;
        boolean showPriority = type == FilterType.PRIORITY;

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
    }

    /**
     * 创建用例表格
     */
    private TableView<TestCase> createCaseTable() {
        TableView<TestCase> table = new TableView<>();
        table.setItems(tableData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 编号列
        TableColumn<TestCase, String> idCol = new TableColumn<>("用例编号");
        idCol.setCellValueFactory(new PropertyValueFactory<>("casesID"));
        idCol.setPrefWidth(120);
        idCol.setStyle("-fx-alignment: CENTER;");

        // 名称列
        TableColumn<TestCase, String> nameCol = new TableColumn<>("用例名称");
        nameCol.setCellValueFactory(cellData -> {
            TestCase tc = cellData.getValue();
            String name = tc.getCaseInfo() != null ? tc.getCaseInfo().getCaseName() : "";
            return new SimpleStringProperty(name);
        });
        nameCol.setPrefWidth(200);
        nameCol.setStyle("-fx-alignment: CENTER-LEFT;");

        // 分组列
        TableColumn<TestCase, String> groupCol = new TableColumn<>("分组");
        groupCol.setCellValueFactory(new PropertyValueFactory<>("group"));
        groupCol.setPrefWidth(100);
        groupCol.setStyle("-fx-alignment: CENTER;");

        // 状态列
        TableColumn<TestCase, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(cellData -> {
            TestCase tc = cellData.getValue();
            return new SimpleStringProperty(tc.getCurrentStatus().getDisplayName());
        });
        statusCol.setPrefWidth(100);
        statusCol.setStyle("-fx-alignment: CENTER;");

        // 严重程度列
        TableColumn<TestCase, String> severityCol = new TableColumn<>("严重程度");
        severityCol.setCellValueFactory(cellData -> {
            TestCase tc = cellData.getValue();
            String severityStr = tc.getSeverity();
            if (severityStr != null) {
                try {
                    Severity severity = Severity.valueOf(severityStr);
                    return new SimpleStringProperty(severity.getDisplayName());
                } catch (IllegalArgumentException e) {
                    return new SimpleStringProperty(severityStr);
                }
            }
            return new SimpleStringProperty("");
        });
        severityCol.setPrefWidth(100);
        severityCol.setStyle("-fx-alignment: CENTER;");

        // 优先级列
        TableColumn<TestCase, String> priorityCol = new TableColumn<>("优先级");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));
        priorityCol.setPrefWidth(80);
        priorityCol.setStyle("-fx-alignment: CENTER;");

        // 操作列
        TableColumn<TestCase, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(60);
        actionCol.setStyle("-fx-alignment: CENTER;");
        actionCol.setCellFactory(param -> new TableCell<TestCase, Void>() {
            private final Button actionBtn = new Button("...");
            private final ContextMenu contextMenu = new ContextMenu();

            {
                MenuItem editItem = new MenuItem("编辑");
                MenuItem deleteItem = new MenuItem("删除");
                MenuItem addToTaskItem = new MenuItem("添加到任务");

                contextMenu.getItems().addAll(editItem, deleteItem, addToTaskItem);

                editItem.setOnAction(e -> {
                    TestCase tc = getTableView().getItems().get(getIndex());
                    if (onEditCase != null) {
                        onEditCase.accept(tc);
                    }
                });

                deleteItem.setOnAction(e -> {
                    TestCase tc = getTableView().getItems().get(getIndex());
                    if (onDeleteCase != null) {
                        onDeleteCase.accept(tc);
                    }
                });

                addToTaskItem.setOnAction(e -> {
                    TestCase tc = getTableView().getItems().get(getIndex());
                    if (onAddToTask != null) {
                        onAddToTask.accept(tc);
                    }
                });

                actionBtn.setOnMouseClicked(e -> {
                    if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                        contextMenu.show(actionBtn, e.getScreenX(), e.getScreenY());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionBtn);
                }
            }
        });

        table.getColumns().addAll(idCol, nameCol, groupCol, statusCol, severityCol, priorityCol, actionCol);
        return table;
    }

    /**
     * 应用筛选条件
     */
    private void applyFilter() {
        FilterType type = filterTypeCombo.getValue();
        List<TestCase> filtered;

        switch (type) {
            case KEYWORD:
                filtered = caseLogic.filterByName(keywordField.getText());
                break;
            case ID_RANGE:
                Integer startId = parseInt(idStartField.getText());
                Integer endId = parseInt(idEndField.getText());
                if (startId != null && endId != null) {
                    filtered = caseLogic.filterByIdRange(startId, endId);
                } else {
                    filtered = caseLogic.getAllCases();
                }
                break;
            case STATUS:
                filtered = caseLogic.filterByStatus(statusCombo.getValue());
                break;
            case SEVERITY:
                filtered = caseLogic.filterBySeverity(severityCombo.getValue());
                break;
            case PRIORITY:
                filtered = caseLogic.filterByPriority(priorityCombo.getValue());
                break;
            default:
                filtered = caseLogic.getAllCases();
        }

        tableData.setAll(filtered);
    }

    /**
     * 重置筛选条件
     */
    private void resetFilter() {
        keywordField.clear();
        idStartField.clear();
        idEndField.clear();
        statusCombo.setValue(null);
        severityCombo.setValue(null);
        priorityCombo.setValue(null);
        filterTypeCombo.setValue(FilterType.KEYWORD);
        refreshTable();
    }

    /**
     * 刷新表格数据
     */
    public void refreshTable() {
        tableData.setAll(caseLogic.getAllCases());
    }

    /**
     * 更新严重程度筛选选项
     */
    public void updateSeverityOptions() {
        List<String> severities = caseLogic.getUniqueSeverities();
        severityCombo.getItems().clear();
        severityCombo.getItems().addAll(severities);
    }

    /**
     * 更新优先级筛选选项
     */
    public void updatePriorityOptions() {
        List<String> priorities = caseLogic.getUniquePriorities();
        priorityCombo.getItems().clear();
        priorityCombo.getItems().addAll(priorities);
    }

    /**
     * 根据文件路径选中用例并滚动到该位置
     */
    public void selectCaseByPath(String filePath) {
        if (filePath == null) {
            return;
        }

        for (TestCase tc : tableData) {
            if (filePath.equals(tc.getFilePath())) {
                caseTable.getSelectionModel().select(tc);
                caseTable.scrollTo(tc);
                break;
            }
        }
    }

    /**
     * 获取选中的用例
     */
    public TestCase getSelectedCase() {
        return caseTable.getSelectionModel().getSelectedItem();
    }

    /**
     * 编辑用例回调
     */
    private java.util.function.Consumer<TestCase> onEditCase;

    public void setOnEditCase(java.util.function.Consumer<TestCase> handler) {
        this.onEditCase = handler;
    }

    /**
     * 删除用例回调
     */
    private Consumer<TestCase> onDeleteCase;

    public void setOnDeleteCase(Consumer<TestCase> handler) {
        this.onDeleteCase = handler;
    }

    /**
     * 添加到任务回调
     */
    private Consumer<TestCase> onAddToTask;

    public void setOnAddToTask(Consumer<TestCase> handler) {
        this.onAddToTask = handler;
    }

    /**
     * 显示用例模板查看对话框（只读 + 复制）
     * 支持 JSON / XML / YAML / Excel(CSV) 格式
     */
    private void showTemplateDialog() {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("用例模板规范");
        dialog.setWidth(720);
        dialog.setHeight(580);

        // 格式选择
        HBox formatBox = new HBox(10);
        formatBox.setPadding(new Insets(10));
        formatBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label formatLabel = new Label("输出格式:");
        ComboBox<String> formatCombo = new ComboBox<>();
        for (java.util.Map.Entry<String, String> entry : CaseTemplateGenerator.SUPPORTED_FORMATS.entrySet()) {
            formatCombo.getItems().add(entry.getKey());
        }
        if (!formatCombo.getItems().isEmpty()) {
            formatCombo.setValue(formatCombo.getItems().get(0)); // 默认JSON
        }

        // 模板预览区域（只读，用户不可修改）
        TextArea templateArea = new TextArea();
        templateArea.setEditable(false);
        templateArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px; "
                + "-fx-control-inner-background: #f8f8f8;");
        templateArea.setWrapText(true);

        // 操作按钮
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10));
        Button copyBtn = new Button("复制模板");
        copyBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        Label hintLabel = new Label("(只读展示，不可编辑)");
        hintLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        Button closeBtn = new Button("关闭");
        btnBox.getChildren().addAll(copyBtn, hintLabel, closeBtn);

        // 布局
        BorderPane root = new BorderPane();
        root.setTop(formatBox);
        root.setCenter(templateArea);
        root.setBottom(btnBox);

        // 切换格式时重新生成
        Runnable generate = () -> {
            String fmt = formatCombo.getValue();
            if (fmt == null) return;
            try {
                String text = CaseTemplateGenerator.generate(fmt);
                templateArea.setText(text);
            } catch (Exception ex) {
                templateArea.setText("// 生成失败: " + ex.getMessage());
            }
        };

        formatCombo.setOnAction(e -> generate.run());

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

        closeBtn.setOnAction(e -> dialog.close());

        // 首次自动生成
        generate.run();

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.show();
    }

    private Integer parseInt(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

package com.autocase.ui;

import com.autocase.entity.CaseInfo;
import com.autocase.entity.CaseStatus;
import com.autocase.entity.Execution;
import com.autocase.entity.ExpectedResult;
import com.autocase.entity.Severity;
import com.autocase.entity.TestCase;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用例编辑对话框
 */
public class CaseEditDialog extends Stage {

    private final TextField casesIDField;
    private final TextField groupField;
    private final TextField versionField;
    private final ComboBox<Severity> severityCombo;
    private final ComboBox<String> priorityCombo;
    private final TextField caseNameField;
    private final TextArea descriptionArea;
    private final TextArea stepsArea;
    private final TextArea preconditionArea;
    private final TextArea expectedDescArea;
    private final TextArea checkpointsArea;
    private final TextArea remarkArea;
    private final TextField timeoutField;
    private final TextField retryCountField;
    private final TextField tagsField;
    private final ComboBox<CaseStatus> statusCombo;

    // 关联用例和互斥用例
    private List<String> associatedCases;
    private List<String> mutuallyExclusiveCases;
    private Button associatedBtn;
    private Button mutuallyExclusiveBtn;

    private final List<TestCase> allCases;
    private final boolean isNewCase;
    private boolean result = false;

    public CaseEditDialog(TestCase testCase, String rootDirectory, List<TestCase> allCases, boolean isNewCase) {
        this.isNewCase = isNewCase;
        this.allCases = allCases != null ? allCases : new ArrayList<>();

        initModality(Modality.APPLICATION_MODAL);
        setTitle(isNewCase ? "新建用例" : "编辑用例");

        // 初始化控件
        casesIDField = new TextField();
        casesIDField.setPromptText("如: LOGIN_001");
        casesIDField.setDisable(!isNewCase);

        groupField = new TextField();
        groupField.setPromptText("如: 功能测试");

        versionField = new TextField();
        versionField.setPromptText("如: v2.3.0");

        severityCombo = new ComboBox<>();
        severityCombo.getItems().addAll(Severity.values());

        priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll("P0", "P1", "P2", "P3");

        caseNameField = new TextField();
        caseNameField.setPromptText("用例名称");

        descriptionArea = new TextArea();
        descriptionArea.setPromptText("用例描述");
        descriptionArea.setPrefRowCount(3);

        stepsArea = new TextArea();
        stepsArea.setPromptText("执行步骤，每行一个步骤");
        stepsArea.setPrefRowCount(5);

        preconditionArea = new TextArea();
        preconditionArea.setPromptText("前置条件");
        preconditionArea.setPrefRowCount(3);

        expectedDescArea = new TextArea();
        expectedDescArea.setPromptText("预期结果描述");
        expectedDescArea.setPrefRowCount(3);

        checkpointsArea = new TextArea();
        checkpointsArea.setPromptText("验证点，每行一个验证点");
        checkpointsArea.setPrefRowCount(4);

        remarkArea = new TextArea();
        remarkArea.setPromptText("备注");
        remarkArea.setPrefRowCount(2);

        timeoutField = new TextField();
        timeoutField.setPromptText("超时时间(秒)");

        retryCountField = new TextField();
        retryCountField.setPromptText("重试次数");

        tagsField = new TextField();
        tagsField.setPromptText("标签，用逗号分隔");

        statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(CaseStatus.values());

        // 关联用例
        associatedCases = new ArrayList<>();
        associatedBtn = new Button("选择关联用例");
        associatedBtn.setOnAction(e -> showCaseSelector("选择关联用例", associatedCases));

        // 互斥用例
        mutuallyExclusiveCases = new ArrayList<>();
        mutuallyExclusiveBtn = new Button("选择互斥用例");
        mutuallyExclusiveBtn.setOnAction(e -> showCaseSelector("选择互斥用例", mutuallyExclusiveCases));

        // 填充已有数据
        if (testCase != null) {
            casesIDField.setText(testCase.getCasesID());
            groupField.setText(testCase.getGroup());
            versionField.setText(testCase.getVersion());
            String severityStr = testCase.getSeverity();
            if (severityStr != null) {
                for (Severity s : Severity.values()) {
                    if (s.name().equals(severityStr) || s.getDisplayName().equals(severityStr) || s.toString().equals(severityStr)) {
                        severityCombo.setValue(s);
                        break;
                    }
                }
            }
            priorityCombo.setValue(testCase.getPriority());

            if (testCase.getCaseInfo() != null) {
                caseNameField.setText(testCase.getCaseInfo().getCaseName());
                descriptionArea.setText(testCase.getCaseInfo().getDescription());
                if (testCase.getCaseInfo().getStepsToReproduce() != null) {
                    stepsArea.setText(String.join("\n", testCase.getCaseInfo().getStepsToReproduce()));
                }
                if (testCase.getCaseInfo().getPrecondition() != null) {
                    preconditionArea.setText(testCase.getCaseInfo().getPrecondition());
                }
                if (testCase.getCaseInfo().getRemark() != null) {
                    remarkArea.setText(testCase.getCaseInfo().getRemark());
                }
            }

            if (testCase.getExpectedResult() != null) {
                expectedDescArea.setText(testCase.getExpectedResult().getDescription());
                if (testCase.getExpectedResult().getCheckpoints() != null) {
                    checkpointsArea.setText(String.join("\n", testCase.getExpectedResult().getCheckpoints()));
                }
            }

            if (testCase.getExecution() != null) {
                if (testCase.getExecution().getTimeout() != null) {
                    timeoutField.setText(String.valueOf(testCase.getExecution().getTimeout()));
                }
                if (testCase.getExecution().getRetryCount() != null) {
                    retryCountField.setText(String.valueOf(testCase.getExecution().getRetryCount()));
                }
                if (testCase.getExecution().getTags() != null) {
                    tagsField.setText(String.join(", ", testCase.getExecution().getTags()));
                }
            }

            statusCombo.setValue(testCase.getCurrentStatus());

            if (testCase.getAssociatedCases() != null) {
                associatedCases = new ArrayList<>(testCase.getAssociatedCases());
            }
            if (testCase.getMutuallyExclusiveCases() != null) {
                mutuallyExclusiveCases = new ArrayList<>(testCase.getMutuallyExclusiveCases());
            }
        }

        updateAssociatedBtnText();
        updateMutuallyExclusiveBtnText();

        buildUI();
    }

    private void updateAssociatedBtnText() {
        if (associatedCases.isEmpty()) {
            associatedBtn.setText("选择关联用例");
        } else {
            associatedBtn.setText("关联用例 (" + associatedCases.size() + ")");
        }
    }

    private void updateMutuallyExclusiveBtnText() {
        if (mutuallyExclusiveCases.isEmpty()) {
            mutuallyExclusiveBtn.setText("选择互斥用例");
        } else {
            mutuallyExclusiveBtn.setText("互斥用例 (" + mutuallyExclusiveCases.size() + ")");
        }
    }

    private void showCaseSelector(String title, List<String> selectedCases) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label hintLabel = new Label("勾选要" + title.replace("选择", "") + "：");

        // 搜索框
        TextField searchField = new TextField();
        searchField.setPromptText("搜索用例名称或编号...");

        ScrollPane scrollPane = new ScrollPane();
        VBox checkBoxContainer = new VBox(5);
        checkBoxContainer.setPadding(new Insets(5));

        // 用例名称到CheckBox的映射
        Map<String, CheckBox> checkBoxMap = new HashMap<>();
        for (TestCase tc : allCases) {
            String caseName = tc.getCaseInfo() != null ? tc.getCaseInfo().getCaseName() : tc.getCasesID();
            String caseId = tc.getCasesID();
            CheckBox cb = new CheckBox(caseName);
            cb.setSelected(selectedCases.contains(caseName));
            cb.setUserData(caseId); // 存储用例编号用于搜索
            checkBoxMap.put(caseName, cb);
            checkBoxContainer.getChildren().add(cb);
        }

        // 搜索过滤
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String keyword = newVal.toLowerCase().trim();
            checkBoxContainer.getChildren().clear();
            for (Map.Entry<String, CheckBox> entry : checkBoxMap.entrySet()) {
                String caseName = entry.getKey();
                String caseId = (String) entry.getValue().getUserData();
                if (keyword.isEmpty()
                        || caseName.toLowerCase().contains(keyword)
                        || (caseId != null && caseId.toLowerCase().contains(keyword))) {
                    checkBoxContainer.getChildren().add(entry.getValue());
                }
            }
        });

        scrollPane.setContent(checkBoxContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);

        Button okBtn = new Button("确定");
        Button cancelBtn = new Button("取消");
        HBox btnBox = new HBox(10, okBtn, cancelBtn);

        okBtn.setOnAction(e -> {
            selectedCases.clear();
            for (CheckBox cb : checkBoxMap.values()) {
                if (cb.isSelected()) {
                    selectedCases.add(cb.getText());
                }
            }
            if (selectedCases == this.associatedCases) {
                updateAssociatedBtnText();
            } else {
                updateMutuallyExclusiveBtnText();
            }
            dialog.close();
        });

        cancelBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(hintLabel, searchField, scrollPane, btnBox);
        Scene scene = new Scene(root, 350, 400);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void buildUI() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(80);
        col0.setPrefWidth(80);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        int row = 0;
        grid.add(new Label("用例编号*:"), 0, row);
        grid.add(casesIDField, 1, row++);

        grid.add(new Label("分组*:"), 0, row);
        grid.add(groupField, 1, row++);

        grid.add(new Label("版本:"), 0, row);
        grid.add(versionField, 1, row++);

        grid.add(new Label("严重程度*:"), 0, row);
        grid.add(severityCombo, 1, row++);

        grid.add(new Label("优先级:"), 0, row);
        grid.add(priorityCombo, 1, row++);

        grid.add(new Label("用例名称*:"), 0, row);
        grid.add(caseNameField, 1, row++);

        grid.add(new Label("描述:"), 0, row);
        grid.add(descriptionArea, 1, row++);

        grid.add(new Label("执行步骤:"), 0, row);
        grid.add(stepsArea, 1, row++);

        grid.add(new Label("前置条件:"), 0, row);
        grid.add(preconditionArea, 1, row++);

        grid.add(new Label("预期结果:"), 0, row);
        grid.add(expectedDescArea, 1, row++);

        grid.add(new Label("验证点:"), 0, row);
        grid.add(checkpointsArea, 1, row++);

        grid.add(new Label("备注:"), 0, row);
        grid.add(remarkArea, 1, row++);

        grid.add(new Label("超时时间(秒):"), 0, row);
        grid.add(timeoutField, 1, row++);

        grid.add(new Label("重试次数:"), 0, row);
        grid.add(retryCountField, 1, row++);

        grid.add(new Label("标签:"), 0, row);
        grid.add(tagsField, 1, row++);

        grid.add(new Label("状态*:"), 0, row);
        grid.add(statusCombo, 1, row++);

        grid.add(new Label("关联用例:"), 0, row);
        grid.add(associatedBtn, 1, row++);

        grid.add(new Label("互斥用例:"), 0, row);
        grid.add(mutuallyExclusiveBtn, 1, row++);

        // 按钮
        Button saveBtn = new Button("保存");
        Button cancelBtn = new Button("取消");

        saveBtn.setOnAction(e -> {
            if (validateInput()) {
                result = true;
                close();
            }
        });

        cancelBtn.setOnAction(e -> {
            result = false;
            close();
        });

        HBox buttonBox = new HBox(10, saveBtn, cancelBtn);
        GridPane.setHgrow(buttonBox, javafx.scene.layout.Priority.ALWAYS);
        grid.add(buttonBox, 1, row);

        Scene scene = new Scene(grid, 600, 750);
        setScene(scene);
    }

    private boolean validateInput() {
        if (casesIDField.getText().trim().isEmpty()) {
            showAlert("用例编号不能为空");
            return false;
        }
        if (groupField.getText().trim().isEmpty()) {
            showAlert("分组不能为空");
            return false;
        }
        if (severityCombo.getValue() == null) {
            showAlert("请选择严重程度");
            return false;
        }
        if (caseNameField.getText().trim().isEmpty()) {
            showAlert("用例名称不能为空");
            return false;
        }
        if (statusCombo.getValue() == null) {
            showAlert("请选择状态");
            return false;
        }
        return true;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("输入验证");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 获取编辑后的用例对象
     */
    public TestCase getEditedCase() {
        TestCase testCase = new TestCase();
        testCase.setCasesID(casesIDField.getText().trim());
        testCase.setGroup(groupField.getText().trim());
        testCase.setVersion(versionField.getText().trim());
        testCase.setSeverity(severityCombo.getValue().name());
        testCase.setPriority(priorityCombo.getValue());

        CaseInfo caseInfo = new CaseInfo();
        caseInfo.setCaseName(caseNameField.getText().trim());
        caseInfo.setDescription(descriptionArea.getText().trim());
        String stepsText = stepsArea.getText().trim();
        if (!stepsText.isEmpty()) {
            caseInfo.setStepsToReproduce(Arrays.asList(stepsText.split("\n")));
        }
        caseInfo.setPrecondition(preconditionArea.getText().trim());
        caseInfo.setRemark(remarkArea.getText().trim());
        testCase.setCaseInfo(caseInfo);

        ExpectedResult expectedResult = new ExpectedResult();
        expectedResult.setDescription(expectedDescArea.getText().trim());
        String checkpointsText = checkpointsArea.getText().trim();
        if (!checkpointsText.isEmpty()) {
            expectedResult.setCheckpoints(Arrays.asList(checkpointsText.split("\n")));
        }
        testCase.setExpectedResult(expectedResult);

        Execution execution = new Execution();
        String timeoutText = timeoutField.getText().trim();
        if (!timeoutText.isEmpty()) {
            try {
                execution.setTimeout(Integer.parseInt(timeoutText));
            } catch (NumberFormatException ignored) {}
        }
        String retryText = retryCountField.getText().trim();
        if (!retryText.isEmpty()) {
            try {
                execution.setRetryCount(Integer.parseInt(retryText));
            } catch (NumberFormatException ignored) {}
        }
        String tagsText = tagsField.getText().trim();
        if (!tagsText.isEmpty()) {
            execution.setTags(Arrays.asList(tagsText.split(",\\s*")));
        }
        testCase.setExecution(execution);

        testCase.setCurrentStatus(statusCombo.getValue());
        testCase.setAssociatedCases(associatedCases);
        testCase.setMutuallyExclusiveCases(mutuallyExclusiveCases);

        return testCase;
    }

    /**
     * 获取对话框结果（用户是否点击了保存）
     */
    public boolean getResult() {
        return result;
    }
}

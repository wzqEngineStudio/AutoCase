package com.autocase.entity;

/**
 * 测试脚本实体
 */
public class TestScript {

    private String filePath;
    private String fileName;
    private String language;
    private String caseName;
    private boolean selected;

    // 排序相关字段
    private String priority; // P0, P1, P2, P3
    private int stepsCount; // 测试步骤数量

    public TestScript() {
    }

    public TestScript(String filePath, String fileName, String language) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.language = language;
        this.selected = true;
        // 提取用例名称（去掉 Test/_test 后缀）
        this.caseName = extractCaseName(fileName);
    }

    private String extractCaseName(String fileName) {
        String name = fileName;
        // 去掉扩展名
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        // 去掉 Test/_test/case/_case 后缀（支持各种组合）
        name = name.replaceAll("(Test|_test|Case|_case|Cases|_cases)$", "");
        return name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getStepsCount() {
        return stepsCount;
    }

    public void setStepsCount(int stepsCount) {
        this.stepsCount = stepsCount;
    }
}

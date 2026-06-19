package com.autocase.entity;

/**
 * 执行结果实体
 */
public class ExecutionResult {

    private String scriptPath;
    private String scriptName;
    private String language;
    private String caseName;
    private String status; // PASSED, FAILED, BLOCKED, NOT_EXECUTED
    private String expectedOutput;
    private String actualOutput;
    private String errorMessage;
    private long executionTime; // 毫秒
    private String testCasePath; // 关联的用例JSON路径
    private String attachmentPath; // 截图/附件路径
    private String priority; // 优先级
    private String severity; // 严重程度

    public ExecutionResult() {
    }

    public ExecutionResult(String scriptPath, String scriptName, String language, String caseName) {
        this.scriptPath = scriptPath;
        this.scriptName = scriptName;
        this.language = language;
        this.caseName = caseName;
        this.status = "NOT_EXECUTED";
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public String getActualOutput() {
        return actualOutput;
    }

    public void setActualOutput(String actualOutput) {
        this.actualOutput = actualOutput;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public String getTestCasePath() {
        return testCasePath;
    }

    public void setTestCasePath(String testCasePath) {
        this.testCasePath = testCasePath;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}

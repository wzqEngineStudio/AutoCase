package com.autocase.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试用例实体 - 对应JSON用例文件
 */
public class TestCase {
    // 基础元数据
    private String casesID;
    private String group;
    private String version;
    private String severity;
    private String priority;

    // 用例信息
    private CaseInfo caseInfo;

    // 预期结果
    private ExpectedResult expectedResult;

    // 执行控制
    private Execution execution;

    // 测试数据（动态字段）
    @JsonAnySetter
    @JsonAnyGetter
    private Map<String, Object> testData = new HashMap<>();

    // 环境约束
    private Map<String, Object> environment;

    // 变更审计
    private Map<String, Object> metadata;

    // 非JSON字段：文件路径（运行时管理）
    @JsonIgnore
    private String filePath;

    // 当前状态（持久化到JSON）
    private CaseStatus currentStatus = CaseStatus.UNVERIFIED;

    // 关联用例（自动化执行时自动衔接）
    private List<String> associatedCases;

    // 互斥用例（条件互斥，无法同时使用）
    private List<String> mutuallyExclusiveCases;

    public TestCase() {}

    public String getCasesID() {
        return casesID;
    }

    public void setCasesID(String casesID) {
        this.casesID = casesID;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public CaseInfo getCaseInfo() {
        return caseInfo;
    }

    public void setCaseInfo(CaseInfo caseInfo) {
        this.caseInfo = caseInfo;
    }

    public ExpectedResult getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(ExpectedResult expectedResult) {
        this.expectedResult = expectedResult;
    }

    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    public Map<String, Object> getTestData() {
        return testData;
    }

    public void setTestData(Map<String, Object> testData) {
        this.testData = testData;
    }

    public Map<String, Object> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, Object> environment) {
        this.environment = environment;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public CaseStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(CaseStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    /**
     * 获取用例名称（便捷方法）
     */
    public String getCaseName() {
        return caseInfo != null ? caseInfo.getCaseName() : "";
    }

    public List<String> getAssociatedCases() {
        return associatedCases;
    }

    public void setAssociatedCases(List<String> associatedCases) {
        this.associatedCases = associatedCases;
    }

    public List<String> getMutuallyExclusiveCases() {
        return mutuallyExclusiveCases;
    }

    public void setMutuallyExclusiveCases(List<String> mutuallyExclusiveCases) {
        this.mutuallyExclusiveCases = mutuallyExclusiveCases;
    }
}

package com.autocase.entity;

import java.sql.Timestamp;

/**
 * 手工测试详情实体 - 对应 manual_test_detail 表
 */
public class ManualTestDetail {
    private Long detailId;
    private Long batchId;
    private String caseId;
    private String caseName;
    private String status; // PASS / FAIL / BLOCKED / PENDING
    private String expectedResult;
    private String actualResult;
    private String failureReason;
    private String attachmentPath; // 截图/录屏路径
    private String remark;
    private String linkedAutoCaseId; // 关联的自动化用例ID
    private String priority; // 优先级
    private String severity; // 严重程度
    private Timestamp executionTime;

    public ManualTestDetail() {}

    public Long getDetailId() {
        return detailId;
    }

    public void setDetailId(Long detailId) {
        this.detailId = detailId;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
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

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getActualResult() {
        return actualResult;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getLinkedAutoCaseId() {
        return linkedAutoCaseId;
    }

    public void setLinkedAutoCaseId(String linkedAutoCaseId) {
        this.linkedAutoCaseId = linkedAutoCaseId;
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

    public Timestamp getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Timestamp executionTime) {
        this.executionTime = executionTime;
    }
}

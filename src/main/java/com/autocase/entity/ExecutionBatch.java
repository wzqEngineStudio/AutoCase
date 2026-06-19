package com.autocase.entity;

import java.sql.Timestamp;

/**
 * 执行批次实体 - 对应 execution_batch 表
 */
public class ExecutionBatch {
    private Long batchId;
    private String triggerType;
    private String triggerUser;
    private Integer totalCases;
    private Integer passedCount;
    private Integer failedCount;
    private Integer blockedCount;
    private Timestamp startTime;
    private Timestamp endTime;
    private String gitCommit;

    public ExecutionBatch() {}

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerUser() {
        return triggerUser;
    }

    public void setTriggerUser(String triggerUser) {
        this.triggerUser = triggerUser;
    }

    public Integer getTotalCases() {
        return totalCases;
    }

    public void setTotalCases(Integer totalCases) {
        this.totalCases = totalCases;
    }

    public Integer getPassedCount() {
        return passedCount;
    }

    public void setPassedCount(Integer passedCount) {
        this.passedCount = passedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public Integer getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(Integer blockedCount) {
        this.blockedCount = blockedCount;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }
}

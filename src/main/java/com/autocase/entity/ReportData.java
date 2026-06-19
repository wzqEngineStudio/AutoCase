package com.autocase.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 测试报告数据实体
 */
public class ReportData {

    private String reportId;
    private String rootDirectory;
    private long startTime;
    private long endTime;
    private int totalScripts;
    private int executedScripts;
    private int passedScripts;
    private int failedScripts;
    private int blockedScripts;
    private int notExecutedScripts;
    private int skippedScripts;
    private int timeoutScripts;
    private double passRate;
    private List<ExecutionResult> results;
    private Map<String, String> baselineSnapshot;

    public ReportData() {
        this.results = new ArrayList<>();
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getTotalScripts() {
        return totalScripts;
    }

    public void setTotalScripts(int totalScripts) {
        this.totalScripts = totalScripts;
    }

    public int getExecutedScripts() {
        return executedScripts;
    }

    public void setExecutedScripts(int executedScripts) {
        this.executedScripts = executedScripts;
    }

    public int getPassedScripts() {
        return passedScripts;
    }

    public void setPassedScripts(int passedScripts) {
        this.passedScripts = passedScripts;
    }

    public int getFailedScripts() {
        return failedScripts;
    }

    public void setFailedScripts(int failedScripts) {
        this.failedScripts = failedScripts;
    }

    public int getBlockedScripts() {
        return blockedScripts;
    }

    public void setBlockedScripts(int blockedScripts) {
        this.blockedScripts = blockedScripts;
    }

    public int getNotExecutedScripts() {
        return notExecutedScripts;
    }

    public void setNotExecutedScripts(int notExecutedScripts) {
        this.notExecutedScripts = notExecutedScripts;
    }

    public int getSkippedScripts() {
        return skippedScripts;
    }

    public void setSkippedScripts(int skippedScripts) {
        this.skippedScripts = skippedScripts;
    }

    public int getTimeoutScripts() {
        return timeoutScripts;
    }

    public void setTimeoutScripts(int timeoutScripts) {
        this.timeoutScripts = timeoutScripts;
    }

    public Map<String, String> getBaselineSnapshot() {
        return baselineSnapshot;
    }

    public void setBaselineSnapshot(Map<String, String> baselineSnapshot) {
        this.baselineSnapshot = baselineSnapshot;
    }

    public double getPassRate() {
        return passRate;
    }

    public void setPassRate(double passRate) {
        this.passRate = passRate;
    }

    public List<ExecutionResult> getResults() {
        return results;
    }

    public void setResults(List<ExecutionResult> results) {
        this.results = results;
    }

    /**
     * 计算统计数据
     */
    public void calculateStats() {
        this.totalScripts = results.size();
        this.executedScripts = 0;
        this.passedScripts = 0;
        this.failedScripts = 0;
        this.blockedScripts = 0;
        this.notExecutedScripts = 0;
        this.skippedScripts = 0;
        this.timeoutScripts = 0;

        for (ExecutionResult result : results) {
            String status = result.getStatus();
            if ("PASSED".equals(status)) {
                passedScripts++;
                executedScripts++;
            } else if ("FAILED".equals(status)) {
                failedScripts++;
                executedScripts++;
            } else if ("BLOCKED".equals(status)) {
                blockedScripts++;
                executedScripts++;
            } else if ("TIMEOUT".equals(status)) {
                timeoutScripts++;
                executedScripts++;
            } else if ("SKIPPED".equals(status)) {
                skippedScripts++;
            } else {
                notExecutedScripts++;
            }
        }

        if (executedScripts > 0) {
            this.passRate = (double) passedScripts / executedScripts * 100;
        } else {
            this.passRate = 0;
        }
    }
}

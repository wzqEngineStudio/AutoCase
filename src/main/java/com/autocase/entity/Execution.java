package com.autocase.entity;

import java.util.List;
import java.util.Map;

/**
 * 执行控制实体
 */
public class Execution {
    private String priority;
    private Integer timeout;
    private Integer retryCount;
    private List<String> tags;
    private List<String> dependsOn;

    public Execution() {}

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }
}

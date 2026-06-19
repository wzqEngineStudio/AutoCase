package com.autocase.entity;

import java.util.List;

/**
 * 预期结果实体
 */
public class ExpectedResult {
    private String description;
    private List<String> checkpoints;

    public ExpectedResult() {}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getCheckpoints() {
        return checkpoints;
    }

    public void setCheckpoints(List<String> checkpoints) {
        this.checkpoints = checkpoints;
    }
}

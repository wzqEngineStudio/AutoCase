package com.autocase.entity;

import java.util.List;

/**
 * 用例信息实体
 */
public class CaseInfo {
    private String caseName;
    private String description;
    private List<String> stepsToReproduce;
    private String precondition;
    private String remark;

    public CaseInfo() {}

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getStepsToReproduce() {
        return stepsToReproduce;
    }

    public void setStepsToReproduce(List<String> stepsToReproduce) {
        this.stepsToReproduce = stepsToReproduce;
    }

    public String getPrecondition() {
        return precondition;
    }

    public void setPrecondition(String precondition) {
        this.precondition = precondition;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

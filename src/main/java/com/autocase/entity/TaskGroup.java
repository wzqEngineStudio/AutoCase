package com.autocase.entity;

import java.sql.Timestamp;

/**
 * 手工测试任务组实体 - 对应 task_group 表
 * 一个任务组包含多个测试任务（ManualTestBatch）
 * FromManager 是一个特殊的系统任务组，自动包含用例管理中扫描到的每个用例（每个用例一个任务）
 */
public class TaskGroup {
    private Long groupId;
    private String groupName;
    private String description;
    private Timestamp createTime;
    private String remark;

    public TaskGroup() {}

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

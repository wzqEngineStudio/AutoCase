package com.autocase.entity;

/**
 * 用例状态枚举
 */
public enum CaseStatus {
    UNVERIFIED("未验证"),
    FAILED_NOT_REGRESSED("失败"),
    PASSED("通过"),
    BLOCKED("阻塞"),
    SKIPPED("跳过");

    private final String displayName;

    CaseStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

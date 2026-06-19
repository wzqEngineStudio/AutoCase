package com.autocase.util;

/**
 * 系统常量
 */
public class Constants {

    // 用例文件识别：以 Case/case 结尾的 json 文件
    public static final String CASE_FILE_SUFFIX = "case.json";

    // 配置文件路径
    public static final String CONFIG_DIR = ".cms";
    public static final String CONFIG_FILE = "cms_config.json";

    // UI样式
    public static final String COLOR_CASE_FILE = "#2e7d32";
    public static final String COLOR_DIR_WITH_CASES = "#2e7d32";
    public static final String COLOR_SCRIPT_FILE = "#4a90d9";
    public static final String COLOR_DIR_WITH_SCRIPTS = "#4a90d9";
    public static final String COLOR_NORMAL_FILE = "#757575";
    public static final String COLOR_FOLDER = "#f5a623";
    public static final String COLOR_FILE = "#4a90d9";

    // 目录历史最大数量
    public static final int MAX_DIRECTORY_HISTORY = 10;

    // 调试日志
    public static final String DEBUG_LOG_DIR = "debug_logs";
    public static final int DEBUG_LOG_RETENTION_DAYS = 7;

    // 执行超时（毫秒）
    public static final int DEFAULT_EXECUTION_TIMEOUT = 60000;

    private Constants() {
        // 工具类不允许实例化
    }
}

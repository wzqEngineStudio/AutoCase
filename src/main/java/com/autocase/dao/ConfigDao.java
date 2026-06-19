package com.autocase.dao;

import com.autocase.entity.GlobalConfig;
import com.autocase.entity.GithubConfig;
import com.autocase.util.Constants;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置数据访问类 - 负责读写系统配置文件
 */
public class ConfigDao {

    private final ObjectMapper objectMapper;
    private final String configFilePath;

    public ConfigDao() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 配置文件保存在用户主目录下
        String userHome = System.getProperty("user.home");
        configFilePath = Paths.get(userHome, Constants.CONFIG_DIR, Constants.CONFIG_FILE).toString();
    }

    /**
     * 加载配置
     * @return 配置实体，不存在则返回默认配置
     */
    public CmsConfig loadConfig() {
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            return new CmsConfig();
        }

        try {
            return objectMapper.readValue(configFile, CmsConfig.class);
        } catch (IOException e) {
            System.err.println("Failed to load config: " + configFilePath);
            e.printStackTrace();
            return new CmsConfig();
        }
    }

    /**
     * 保存配置
     * @param config 配置实体
     * @return 是否保存成功
     */
    public boolean saveConfig(CmsConfig config) {
        try {
            // 确保目录存在
            File configFile = new File(configFilePath);
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            objectMapper.writeValue(configFile, config);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save config: " + configFilePath);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 配置实体
     */
    public static class CmsConfig {
        private String lastRootDirectory;
        private String lastViewedCasePath;
        private GithubConfig githubConfig;
        private List<String> directoryHistory;
        private String lastViewedScriptPath;
        private int executionTimeout;
        private List<String> lastScannedScripts;
        private String lastReportId;
        private GlobalConfig globalConfig;

        public CmsConfig() {
            this.lastRootDirectory = null;
            this.lastViewedCasePath = null;
            this.githubConfig = new GithubConfig();
            this.directoryHistory = new ArrayList<>();
            this.executionTimeout = com.autocase.util.Constants.DEFAULT_EXECUTION_TIMEOUT;
            this.lastScannedScripts = new ArrayList<>();
            this.lastReportId = null;
            this.globalConfig = new GlobalConfig();
        }

        public String getLastRootDirectory() {
            return lastRootDirectory;
        }

        public void setLastRootDirectory(String lastRootDirectory) {
            this.lastRootDirectory = lastRootDirectory;
        }

        public String getLastViewedCasePath() {
            return lastViewedCasePath;
        }

        public void setLastViewedCasePath(String lastViewedCasePath) {
            this.lastViewedCasePath = lastViewedCasePath;
        }

        public GithubConfig getGithubConfig() {
            return githubConfig;
        }

        public void setGithubConfig(GithubConfig githubConfig) {
            this.githubConfig = githubConfig;
        }

        public List<String> getDirectoryHistory() {
            if (directoryHistory == null) {
                directoryHistory = new ArrayList<>();
            }
            return directoryHistory;
        }

        public void setDirectoryHistory(List<String> directoryHistory) {
            this.directoryHistory = directoryHistory;
        }

        /**
         * 添加目录到历史记录
         */
        public void addToDirectoryHistory(String directory) {
            if (directory == null || directory.isEmpty()) {
                return;
            }
            directoryHistory.remove(directory);
            directoryHistory.add(0, directory);
            while (directoryHistory.size() > Constants.MAX_DIRECTORY_HISTORY) {
                directoryHistory.remove(directoryHistory.size() - 1);
            }
        }

        public String getLastViewedScriptPath() {
            return lastViewedScriptPath;
        }

        public void setLastViewedScriptPath(String lastViewedScriptPath) {
            this.lastViewedScriptPath = lastViewedScriptPath;
        }

        public int getExecutionTimeout() {
            return executionTimeout;
        }

        public void setExecutionTimeout(int executionTimeout) {
            this.executionTimeout = executionTimeout;
        }

        public List<String> getLastScannedScripts() {
            if (lastScannedScripts == null) {
                lastScannedScripts = new ArrayList<>();
            }
            return lastScannedScripts;
        }

        public void setLastScannedScripts(List<String> lastScannedScripts) {
            this.lastScannedScripts = lastScannedScripts;
        }

        public String getLastReportId() {
            return lastReportId;
        }

        public void setLastReportId(String lastReportId) {
            this.lastReportId = lastReportId;
        }

        public GlobalConfig getGlobalConfig() {
            if (globalConfig == null) {
                globalConfig = new GlobalConfig();
            }
            return globalConfig;
        }

        public void setGlobalConfig(GlobalConfig globalConfig) {
            this.globalConfig = globalConfig;
        }
    }

    /**
     * 获取全局配置
     */
    public GlobalConfig getGlobalConfig() {
        return loadConfig().getGlobalConfig();
    }

    /**
     * 保存全局配置
     */
    public boolean saveGlobalConfig(GlobalConfig globalConfig) {
        CmsConfig config = loadConfig();
        config.setGlobalConfig(globalConfig);
        return saveConfig(config);
    }
}

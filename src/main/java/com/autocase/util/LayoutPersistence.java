package com.autocase.util;

import com.autocase.dao.ConfigDao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 布局持久化工具类
 * 负责保存和恢复各面板的 SplitPane 分割位置
 */
public class LayoutPersistence {

    private static final String LAYOUT_DIR = "AutoCase";
    private static final String LAYOUT_FILE = "layout_positions.json";

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 获取布局文件路径
     */
    private static String getLayoutFilePath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, LAYOUT_DIR, LAYOUT_FILE).toString();
    }

    /**
     * 加载所有布局位置
     */
    private static Map<String, double[]> loadAll() {
        File file = new File(getLayoutFilePath());
        if (!file.exists()) {
            return new HashMap<>();
        }
        try {
            return mapper.readValue(file, new TypeReference<Map<String, double[]>>() {});
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    /**
     * 保存所有布局位置
     */
    private static void saveAll(Map<String, double[]> positions) {
        try {
            File file = new File(getLayoutFilePath());
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, positions);
        } catch (IOException e) {
            System.err.println("Failed to save layout positions: " + e.getMessage());
        }
    }

    /**
     * 获取指定面板的分割位置
     * @param panelKey 面板标识（如 "MainWindow", "ManualTestPanel"）
     * @param defaultPositions 默认分割位置
     * @return 保存的分割位置，不存在则返回默认值
     */
    public static double[] getPositions(String panelKey, double[] defaultPositions) {
        Map<String, double[]> all = loadAll();
        return all.getOrDefault(panelKey, defaultPositions);
    }

    /**
     * 保存指定面板的分割位置
     * @param panelKey 面板标识
     * @param positions 分割位置数组
     */
    public static void savePositions(String panelKey, double[] positions) {
        Map<String, double[]> all = loadAll();
        all.put(panelKey, positions);
        saveAll(all);
    }
}

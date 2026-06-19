package com.autocase.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 文件操作工具类
 */
public class FileUtil {

    private FileUtil() {
        // 工具类不允许实例化
    }

    /**
     * 递归复制目录
     */
    public static boolean copyDirectory(File source, File target) {
        if (!target.exists()) {
            if (!target.mkdir()) {
                return false;
            }
        }
        File[] files = source.listFiles();
        if (files == null) {
            return true;
        }
        for (File file : files) {
            File targetFile = new File(target, file.getName());
            if (file.isDirectory()) {
                if (!copyDirectory(file, targetFile)) {
                    return false;
                }
            } else {
                try {
                    Files.copy(file.toPath(), targetFile.toPath());
                } catch (IOException e) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 递归删除目录
     */
    public static boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return directory.delete();
    }

    /**
     * 判断文件是否为用例文件（文件名以 Case/case 结尾的 json/xml/yaml 文件）
     */
    public static boolean isCaseFile(File file) {
        if (!file.isFile()) {
            return false;
        }
        String name = file.getName().toLowerCase();
        return name.endsWith("case.json") || name.endsWith("case.xml") || 
               name.endsWith("case.yaml") || name.endsWith("case.yml");
    }

    /**
     * 判断目录是否包含用例文件（递归检查）
     */
    public static boolean containsCaseFiles(File directory) {
        if (!directory.isDirectory()) {
            return false;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }
        for (File file : files) {
            if (isCaseFile(file)) {
                return true;
            }
            if (file.isDirectory() && containsCaseFiles(file)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文件是否为测试脚本（文件名以 Test 结尾）
     */
    public static boolean isScriptFile(File file) {
        if (!file.isFile()) {
            return false;
        }
        String name = file.getName();
        // 去掉扩展名后判断是否以 Test 结尾
        int dotIndex = name.lastIndexOf('.');
        String baseName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        return baseName.endsWith("Test");
    }

    /**
     * 判断目录是否包含测试脚本（递归检查）
     */
    public static boolean containsScriptFiles(File directory) {
        if (!directory.isDirectory()) {
            return false;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }
        for (File file : files) {
            if (isScriptFile(file)) {
                return true;
            }
            if (file.isDirectory() && containsScriptFiles(file)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 创建目录
     */
    public static boolean createDirectory(File parentDir, String dirName) {
        if (dirName == null || dirName.trim().isEmpty()) {
            return false;
        }
        File newDir = new File(parentDir, dirName.trim());
        return newDir.mkdir();
    }

    /**
     * 重命名文件/目录
     */
    public static boolean renameFile(File file, String newName) {
        if (newName == null || newName.trim().isEmpty() || newName.equals(file.getName())) {
            return false;
        }
        File newFile = new File(file.getParent(), newName.trim());
        return file.renameTo(newFile);
    }
}

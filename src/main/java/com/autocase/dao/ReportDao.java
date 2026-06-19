package com.autocase.dao;

import com.autocase.entity.ExecutionResult;
import com.autocase.entity.ReportData;
import com.autocase.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试报告数据访问类 - 负责保存和加载测试报告
 */
public class ReportDao {

    private static final String REPORT_DIR = "cms_reports";
    private final ObjectMapper objectMapper;
    private final String reportDirPath;

    public ReportDao() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // 报告保存在用户主目录下
        String userHome = System.getProperty("user.home");
        reportDirPath = Paths.get(userHome, Constants.CONFIG_DIR, REPORT_DIR).toString();
    }

    /**
     * 保存报告
     */
    public boolean saveReport(ReportData report) {
        try {
            File dir = new File(reportDirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = "report_" + report.getReportId() + ".json";
            File reportFile = new File(dir, fileName);
            objectMapper.writeValue(reportFile, report);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 加载指定报告
     */
    public ReportData loadReport(String reportId) {
        try {
            String fileName = "report_" + reportId + ".json";
            File reportFile = new File(reportDirPath, fileName);
            if (reportFile.exists()) {
                return objectMapper.readValue(reportFile, ReportData.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取所有报告ID列表
     */
    public List<String> getReportIds() {
        List<String> ids = new ArrayList<>();
        File dir = new File(reportDirPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.startsWith("report_") && name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    // 提取 report_xxx.json 中的 xxx
                    String id = name.substring(7, name.length() - 5);
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    /**
     * 生成报告ID（时间戳格式）
     */
    public static String generateReportId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}

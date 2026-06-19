package com.autocase.dao;

import com.autocase.entity.TestCase;
import com.autocase.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 用例数据访问类 - 支持 JSON/XML/YAML 多格式用例
 */
public class CaseDao {

    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;
    private final YAMLMapper yamlMapper;

    public CaseDao() {
        // JSON Mapper
        jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // XML Mapper
        xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // YAML Mapper
        yamlMapper = new YAMLMapper();
        yamlMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 扫描指定目录下的所有用例文件（支持 json/xml/yaml）
     * @param directoryPath 目录绝对路径
     * @return 用例列表
     */
    public List<TestCase> scanCases(String directoryPath) {
        List<TestCase> cases = new ArrayList<>();
        Path rootPath = Paths.get(directoryPath);

        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            return cases;
        }

        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.filter(Files::isRegularFile)
                .filter(path -> isCaseFile(path))
                .forEach(path -> {
                    TestCase testCase = loadCase(path.toString());
                    if (testCase != null) {
                        cases.add(testCase);
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cases;
    }

    /**
     * 判断是否为用例文件（文件名以 Case/case 结尾的 json/xml/yaml 文件）
     */
    private boolean isCaseFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return (fileName.endsWith("case.json") || 
                fileName.endsWith("case.xml") || 
                fileName.endsWith("case.yaml") ||
                fileName.endsWith("case.yml"));
    }

    /**
     * 加载单个用例文件（自动识别格式）
     * @param filePath 文件绝对路径
     * @return 用例实体，解析失败返回null
     */
    public TestCase loadCase(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }

            TestCase testCase;
            String lowerPath = filePath.toLowerCase();
            
            if (lowerPath.endsWith(".json")) {
                testCase = jsonMapper.readValue(file, TestCase.class);
            } else if (lowerPath.endsWith(".xml")) {
                testCase = xmlMapper.readValue(file, TestCase.class);
            } else if (lowerPath.endsWith(".yaml") || lowerPath.endsWith(".yml")) {
                testCase = yamlMapper.readValue(file, TestCase.class);
            } else {
                return null;
            }
            
            testCase.setFilePath(filePath);
            return testCase;
        } catch (IOException e) {
            System.err.println("Failed to load case file: " + filePath);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 保存用例到文件（根据文件扩展名自动选择格式）
     * @param testCase 用例实体
     * @return 是否保存成功
     */
    public boolean saveCase(TestCase testCase) {
        if (testCase == null || testCase.getFilePath() == null) {
            return false;
        }

        try {
            File file = new File(testCase.getFilePath());
            String lowerPath = testCase.getFilePath().toLowerCase();
            
            if (lowerPath.endsWith(".json")) {
                jsonMapper.writeValue(file, testCase);
            } else if (lowerPath.endsWith(".xml")) {
                xmlMapper.writeValue(file, testCase);
            } else if (lowerPath.endsWith(".yaml") || lowerPath.endsWith(".yml")) {
                yamlMapper.writeValue(file, testCase);
            } else {
                // 默认 JSON
                jsonMapper.writeValue(file, testCase);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save case file: " + testCase.getFilePath());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 创建新用例文件（根据指定格式）
     * @param directoryPath 目录路径
     * @param testCase 用例实体
     * @param format 格式 (json/xml/yaml)
     * @return 是否创建成功
     */
    public boolean createCase(String directoryPath, TestCase testCase, String format) {
        String caseName = testCase.getCaseInfo().getCaseName();
        String snakeName = camelToSnake(caseName);
        String extension = getExtensionForFormat(format);
        String fileName = snakeName + "Case" + extension;
        String filePath = Paths.get(directoryPath, fileName).toString();
        testCase.setFilePath(filePath);
        return saveCase(testCase);
    }

    /**
     * 创建新用例文件（使用默认格式 JSON）
     */
    public boolean createCase(String directoryPath, TestCase testCase) {
        return createCase(directoryPath, testCase, "json");
    }

    /**
     * 根据格式获取文件扩展名
     */
    private String getExtensionForFormat(String format) {
        switch (format) {
            case "xml": return ".xml";
            case "yaml": return ".yaml";
            case "excel": return ".xlsx"; // 暂未实现
            default: return ".json";
        }
    }

    /**
     * 驼峰转下划线
     */
    private String camelToSnake(String camel) {
        if (camel == null) return "";
        return camel.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * 删除用例文件
     * @param testCase 用例实体
     * @return 是否删除成功
     */
    public boolean deleteCase(TestCase testCase) {
        if (testCase == null || testCase.getFilePath() == null) {
            return false;
        }

        try {
            File file = new File(testCase.getFilePath());
            return file.delete();
        } catch (Exception e) {
            System.err.println("Failed to delete case file: " + testCase.getFilePath());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取目录下的所有子目录
     * @param directoryPath 目录路径
     * @return 子目录列表
     */
    public List<String> getSubDirectories(String directoryPath) {
        List<String> directories = new ArrayList<>();
        Path rootPath = Paths.get(directoryPath);

        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            return directories;
        }

        try (Stream<Path> walk = Files.walk(rootPath, 1)) {
            walk.filter(Files::isDirectory)
                .filter(path -> !path.equals(rootPath))
                .forEach(path -> directories.add(path.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return directories;
    }
}

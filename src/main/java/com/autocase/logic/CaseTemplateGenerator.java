package com.autocase.logic;

import com.autocase.entity.TestCase;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用例模板生成器 - 生成标准用例模板（JSON / XML / YAML / Excel格式）
 * 展示用例必须拥有的数据规范，只读展示，提供复制功能
 */
public class CaseTemplateGenerator {

    /** 支持的输出格式 */
    public static final Map<String, String> SUPPORTED_FORMATS = new LinkedHashMap<>();

    static {
        SUPPORTED_FORMATS.put("json", "JSON");
        SUPPORTED_FORMATS.put("xml", "XML");
        SUPPORTED_FORMATS.put("yaml", "YAML");
        SUPPORTED_FORMATS.put("excel", "Excel (CSV)");
    }

    private CaseTemplateGenerator() {}

    /**
     * 生成指定格式的用例模板文本
     *
     * @param format 格式标识 (json/xml/yaml/excel)
     * @return 模板文本
     */
    public static String generate(String format) {
        switch (format) {
            case "json": return generateJsonTemplate();
            case "xml":  return generateXmlTemplate();
            case "yaml": return generateYamlTemplate();
            case "excel": return generateExcelCsvTemplate();
            default:
                throw new IllegalArgumentException("不支持的格式: " + format
                        + "，支持: " + String.join(", ", SUPPORTED_FORMATS.keySet()));
        }
    }

    // ==================== JSON 模板 ====================

    private static String generateJsonTemplate() {
        return "{\n"
                + "  \"casesID\": \"TEST_001\",\n"
                + "  \"group\": \"模块名称\",\n"
                + "  \"version\": \"1.0\",\n"
                + "  \"severity\": \"S2\",\n"
                + "  \"priority\": \"P1\",\n"
                + "  \n"
                + "  \"caseInfo\": {\n"
                + "    \"caseName\": \"测试用例名称\",\n"
                + "    \"description\": \"用例描述，说明测试目的和范围\",\n"
                + "    \"stepsToReproduce\": [\n"
                + "      \"步骤1: 前置操作\",\n"
                + "      \"步骤2: 执行动作\",\n"
                + "      \"步骤3: 验证结果\"\n"
                + "    ],\n"
                + "    \"precondition\": \"前置条件说明（环境、数据、权限等）\",\n"
                + "    \"remark\": \"备注信息\"\n"
                + "  },\n"
                + "  \n"
                + "  \"expectedResult\": {\n"
                + "    \"description\": \"预期结果描述\",\n"
                + "    \"checkpoints\": [\n"
                + "      \"检查点1: 具体验证项\",\n"
                + "      \"检查点2: 具体验证项\"\n"
                + "    ]\n"
                + "  },\n"
                + "  \n"
                + "  \"execution\": {\n"
                + "    \"priority\": \"P1\",\n"
                + "    \"timeout\": 30,\n"
                + "    \"retryCount\": 1,\n"
                + "    \"tags\": [\"smoke\", \"regression\"],\n"
                + "    \"dependsOn\": [\"TEST_000\"]\n"
                + "  },\n"
                + "  \n"
                + "  \"currentStatus\": \"UNVERIFIED\",\n"
                + "  \"associatedCases\": [\"TEST_002\"],\n"
                + "  \"mutuallyExclusiveCases\": [],\n"
                + "  \n"
                + "  \"testData\": {},\n"
                + "  \"environment\": {\n"
                + "    \"os\": \"Windows/Linux/macOS\",\n"
                + "    \"browser\": \"Chrome/Edge/Firefox\",\n"
                + "    \"version\": \"应用版本号\"\n"
                + "  }\n"
                + "}\n";
    }

    // ==================== XML 模板 ====================

    private static String generateXmlTemplate() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!-- CMS 测试用例模板 -->\n"
                + "<testCase>\n"
                + "  <!-- 基础元数据 -->\n"
                + "  <casesID>TEST_001</casesID>\n"
                + "  <group>模块名称</group>\n"
                + "  <version>1.0</version>\n"
                + "  <severity>S2</severity>\n"
                + "  <priority>P1</priority>\n"
                + "\n"
                + "  <!-- 用例信息 -->\n"
                + "  <caseInfo>\n"
                + "    <caseName>测试用例名称</caseName>\n"
                + "    <description>用例描述，说明测试目的和范围</description>\n"
                + "    <stepsToReproduce>\n"
                + "      <step>步骤1: 前置操作</step>\n"
                + "      <step>步骤2: 执行动作</step>\n"
                + "      <step>步骤3: 验证结果</step>\n"
                + "    </stepsToReproduce>\n"
                + "    <precondition>前置条件说明（环境、数据、权限等）</precondition>\n"
                + "    <remark>备注信息</remark>\n"
                + "  </caseInfo>\n"
                + "\n"
                + "  <!-- 预期结果 -->\n"
                + "  <expectedResult>\n"
                + "    <description>预期结果描述</description>\n"
                + "    <checkpoints>\n"
                + "      <checkpoint>检查点1: 具体验证项</checkpoint>\n"
                + "      <checkpoint>检查点2: 具体验证项</checkpoint>\n"
                + "    </checkpoints>\n"
                + "  </expectedResult>\n"
                + "\n"
                + "  <!-- 执行控制 -->\n"
                + "  <execution>\n"
                + "    <priority>P1</priority>\n"
                + "    <timeout>30</timeout>\n"
                + "    <retryCount>1</retryCount>\n"
                + "    <tags>\n"
                + "      <tag>smoke</tag>\n"
                + "      <tag>regression</tag>\n"
                + "    </tags>\n"
                + "    <dependsOn>\n"
                + "      <case>TEST_000</case>\n"
                + "    </dependsOn>\n"
                + "  </execution>\n"
                + "\n"
                + "  <!-- 状态与关联 -->\n"
                + "  <currentStatus>UNVERIFIED</currentStatus>\n"
                + "  <associatedCases>\n"
                + "    <case>TEST_002</case>\n"
                + "  </associatedCases>\n"
                + "\n"
                + "  <!-- 环境约束 -->\n"
                + "  <environment>\n"
                + "    <os>Windows/Linux/macOS</os>\n"
                + "    <browser>Chrome/Edge/Firefox</browser>\n"
                + "    <version>应用版本号</version>\n"
                + "  </environment>\n"
                + "</testCase>";
    }

    // ==================== YAML 模板 ====================

    private static String generateYamlTemplate() {
        return "# ================================\n"
                + "# CMS 测试用例模板\n"
                + "# ================================\n"
                + "\n"
                + "# --- 基础元数据 ---\n"
                + "casesID: \"TEST_001\"\n"
                + "group: \"模块名称\"\n"
                + "version: \"1.0\"\n"
                + "severity: \"S2\"          # S1=Fatal, S2=Critical, S3=Major, S4=Minor\n"
                + "priority: \"P1\"         # P0=致命, P1=严重, P2=一般, P3=轻微\n"
                + "\n"
                + "# --- 用例信息 ---\n"
                + "caseInfo:\n"
                + "  caseName: \"测试用例名称\"\n"
                + "  description: \"用例描述，说明测试目的和范围\"\n"
                + "  stepsToReproduce:\n"
                + "    - \"步骤1: 前置操作\"\n"
                + "    - \"步骤2: 执行动作\"\n"
                + "    - \"步骤3: 验证结果\"\n"
                + "  precondition: \"前置条件说明（环境、数据、权限等）\"\n"
                + "  remark: \"备注信息\"\n"
                + "\n"
                + "# --- 预期结果 ---\n"
                + "expectedResult:\n"
                + "  description: \"预期结果描述\"\n"
                + "  checkpoints:\n"
                + "    - \"检查点1: 具体验证项\"\n"
                + "    - \"检查点2: 具体验证项\"\n"
                + "\n"
                + "# --- 执行控制 ---\n"
                + "execution:\n"
                + "  priority: \"P1\"\n"
                + "  timeout: 30             # 超时时间(秒)\n"
                + "  retryCount: 1           # 失败重试次数\n"
                + "  tags:\n"
                + "    - \"smoke\"\n"
                + "    - \"regression\"\n"
                + "  dependsOn:\n"
                + "    - \"TEST_000\"\n"
                + "\n"
                + "# --- 状态 ---\n"
                + "currentStatus: \"UNVERIFIED\"   # UNVERIFIED/PASSED/FAILED/BLOCKED\n"
                + "\n"
                + "# --- 关联用例 ---\n"
                + "associatedCases:\n"
                + "  - \"TEST_002\"\n"
                + "mutuallyExclusiveCases: []\n"
                + "\n"
                + "# --- 自定义测试数据 ---\n"
                + "testData: {}\n"
                + "\n"
                + "# --- 环境约束 ---\n"
                + "environment:\n"
                + "  os: \"Windows/Linux/macOS\"\n"
                + "  browser: \"Chrome/Edge/Firefox\"\n"
                + "  version: \"应用版本号\"";
    }

    // ==================== Excel (CSV) 模板 ====================

    private static String generateExcelCsvTemplate() {
        return "# Excel 用例模板 (可复制到 .xlsx 或 .csv)\n"
                + "# 字段说明见下方注释\n"
                + "\n"
                + "# ========== 表头（直接复制到Excel第一行）==========\n"
                + "用例编号,用例名称,分组,版本,严重程度,优先级,"
                + "描述,前置条件,测试步骤,预期结果,检查点,"
                + "超时(秒),重试次数,标签,依赖用例,状态,备注,环境-OS,环境-浏览器,环境-版本\n"
                + "\n"
                + "# ========== 示例行 ==========\n"
                + "TEST_001,登录功能测试,用户模块,1.0,S2,P1,"
                + "\"验证用户使用正确账号密码可以成功登录系统\",\"用户已注册且账号状态正常\","
                + "\"1.打开登录页面;2.输入用户名;3.输入密码;4.点击登录按钮\","
                + "\"跳转到首页且显示用户信息\",\"1.页面跳转正确;2.用户名显示正确\","
                + "30,1,\"smoke,regression\",,UNVERIFIED,,Windows,Chrome,v1.2.0\n"
                + "\n"
                + "# ========== 字段规范 ==========\n"
                + "# 用例编号   : 必填 | 格式 TEST_NNN 或模块缩写_NNN\n"
                + "# 用例名称   : 必填 | 简明扼要说明测试内容\n"
                + "# 分组       : 必填 | 所属模块或功能域\n"
                + "# 版本       : 必填 | 默认 1.0\n"
                + "# 严重程度   : 必填 | S1=Fatal / S2=Critical / S3=Major / S4=Minor\n"
                + "# 优先级     : 必填 | P0=致命 / P1=严重 / P2=一般 / P3=轻微\n"
                + "# 描述       : 必填 | 说明测试目的和范围\n"
                + "# 前置条件   : 可选 | 环境、数据、权限等前提要求\n"
                + "# 测试步骤   : 必填 | 分号分隔每个步骤\n"
                + "# 预期结果   : 必填 | 期望的执行结果\n"
                + "# 检查点     : 可选 | 分号分隔具体验证项\n"
                + "# 超时(秒)   : 可选 | 默认30\n"
                + "# 重试次数   : 可选 | 默认1\n"
                + "# 标签       : 可选 | 逗号分隔，如 smoke,regression\n"
                + "# 依赖用例   : 可选 | 逗号分隔前置依赖的用例编号\n"
                + "# 状态       : 必填 | UNVERIFIED/PASSED/FAILED/BLOCKED\n"
                + "# 备注       : 可选 | 补充说明\n"
                + "# 环境-OS    : 可选 | Windows/Linux/macOS\n"
                + "# 环境-浏览器 : 可选 | Chrome/Edge/Firefox\n"
                + "# 环境-版本  : 可选 | 被测应用版本号";
    }
}

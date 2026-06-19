package com.autocase.logic;

import com.autocase.entity.TestCase;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 脚本模板生成器 - 根据用例数据生成各语言的测试脚本模板
 * 只规范每个用例必须拥有的数据字段，不规范执行过程
 * 不同语言的模板逻辑相同，仅语法不同
 */
public class ScriptTemplateGenerator {

    /** 支持的语言及其文件扩展名 */
    public static final Map<String, String> SUPPORTED_LANGUAGES = new LinkedHashMap<>();

    static {
        SUPPORTED_LANGUAGES.put("python", "Python (.py)");
        SUPPORTED_LANGUAGES.put("java", "Java (.java)");
        SUPPORTED_LANGUAGES.put("cpp", "C++ (.cpp)");
        SUPPORTED_LANGUAGES.put("csharp", "C# (.cs)");
        SUPPORTED_LANGUAGES.put("gdscript", "GDScript (.gd)");
    }

    private ScriptTemplateGenerator() {}

    /**
     * 生成指定语言的脚本模板
     *
     * @param testCase 用例数据（必须字段：casesID, caseName, description, steps, precondition,
     *                 expectedDesc, checkpoints, priority, timeout, retryCount, tags）
     * @param language 语言标识（python/java/cpp/csharp/gdscript）
     * @return 生成的脚本模板文本
     */
    public static String generate(TestCase testCase, String language) {
        if (testCase == null) {
            throw new IllegalArgumentException("用例数据不能为空");
        }
        switch (language) {
            case "python": return generatePython(testCase);
            case "java":   return generateJava(testCase);
            case "cpp":   return generateCpp(testCase);
            case "csharp": return generateCSharp(testCase);
            case "gdscript": return generateGdscript(testCase);
            default:
                throw new IllegalArgumentException("不支持的语言: " + language
                        + "，支持: " + String.join(", ", SUPPORTED_LANGUAGES.keySet()));
        }
    }

    /**
     * 生成不关联用例的独立脚本模板（CASE_NAME 为空串，其他元数据为默认值）
     * 用户可手动编辑 CASE_NAME 后保存
     *
     * @param language 语言标识
     * @return 空壳脚本模板文本
     */
    public static String generateStandalone(String language) {
        switch (language) {
            case "python": return generateStandalonePython();
            case "java":   return generateStandaloneJava();
            case "cpp":   return generateStandaloneCpp();
            case "csharp": return generateStandaloneCSharp();
            case "gdscript": return generateStandaloneGdscript();
            default:
                throw new IllegalArgumentException("不支持的语言: " + language);
        }
    }

    // ==================== Python 模板 ====================

    private static String generatePython(TestCase tc) {
        StringBuilder sb = new StringBuilder();
        sb.append("#!/usr/bin/env python3\n");
        sb.append("# -*- coding: utf-8 -*-\n");
        sb.append("\"\"\"\n");
        sb.append("测试脚本: ").append(escape(tc.getCaseName())).append("\n");
        sb.append("用例编号: ").append(escape(tc.getCasesID())).append("\n");
        sb.append("优先级:   ").append(escape(tc.getPriority())).append("\n");
        sb.append("超时:     ").append(getTimeout(tc)).append("秒\n");
        sb.append("重试:     ").append(getRetryCount(tc)).append("次\n");
        if (hasTags(tc)) {
            sb.append("标签:     ").append(escape(joinTags(tc))).append("\n");
        }
        sb.append("\"\"\"\n\n");

        sb.append("import unittest\n");
        sb.append("import time\n\n");

        // 用例元数据
        sb.append("# ========== 用例元数据 ==========\n");
        sb.append("CASE_ID = \"").append(escape(tc.getCasesID())).append("\"\n");
        sb.append("CASE_NAME = \"").append(escape(tc.getCaseName())).append("\"\n");
        sb.append("PRIORITY = \"").append(escape(tc.getPriority())).append("\"\n");
        sb.append("TIMEOUT = ").append(getTimeout(tc)).append("\n");
        sb.append("RETRY_COUNT = ").append(getRetryCount(tc)).append("\n");
        if (hasTags(tc)) {
            sb.append("TAGS = [");
            String[] tags = getTagArray(tc);
            for (int i = 0; i < tags.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(escape(tags[i])).append("\"");
            }
            sb.append("]\n");
        }
        sb.append("\n");

        // 前置条件
        sb.append("# ========== 前置条件 ==========\n");
        String precond = getPrecondition(tc);
        if (precond != null && !precond.isEmpty()) {
            sb.append("def setup_precondition():\n");
            sb.append("    \"\"\"").append(escape(precond)).append("\"\"\"\n");
            sb.append("    # TODO: 实现前置条件\n");
            sb.append("    pass\n\n");
        } else {
            sb.append("# 无前置条件\n\n");
        }

        // 测试步骤
        sb.append("# ========== 测试步骤 ==========\n");
        String[] steps = getSteps(tc);
        for (int i = 0; i < steps.length; i++) {
            sb.append("def step_").append(i + 1).append("():\n");
            sb.append("    \"\"\"步骤").append(i + 1).append(": ").append(escape(steps[i])).append("\"\"\"\n");
            sb.append("    # TODO: 实现步骤").append(i + 1).append("\n");
            sb.append("    pass\n\n");
        }

        // 预期结果
        sb.append("# ========== 预期结果 ==========\n");
        String expectedDesc = getExpectedDescription(tc);
        sb.append("EXPECTED_RESULT = \"\"\"").append(expectedDesc != null ? escape(expectedDesc) : "").append("\"\"\"\n");
        String[] checkpoints = getCheckpoints(tc);
        if (checkpoints != null && checkpoints.length > 0) {
            sb.append("CHECKPOINTS = [\n");
            for (String cp : checkpoints) {
                sb.append("    \"").append(escape(cp)).append("\",\n");
            }
            sb.append("]\n");
        }
        sb.append("\n");

        // 测试执行
        sb.append("# ========== 测试执行 ==========\n");
        sb.append("class Test").append(sanitizeName(tc.getCaseName())).append("(unittest.TestCase):\n\n");
        sb.append("    def setUp(self):\n");
        sb.append("        self.start_time = time.time()\n");
        if (precond != null && !precond.isEmpty()) {
            sb.append("        setup_precondition()\n");
        }
        sb.append("\n");
        sb.append("    def test_").append(sanitizeName(tc.getCaseName())).append("(self):\n");
        sb.append("        \"\"\"执行测试: ").append(escape(tc.getCaseName())).append("\"\"\"\n");
        for (int i = 0; i < steps.length; i++) {
            sb.append("        step_").append(i + 1).append("()\n");
        }
        sb.append("\n");
        sb.append("    def tearDown(self):\n");
        sb.append("        elapsed = time.time() - self.start_time\n");
        sb.append("        print(f\"[执行耗时] {elapsed:.2f}秒\")\n");
        sb.append("\n");

        sb.append("if __name__ == \"__main__\":\n");
        sb.append("    unittest.main()\n");

        return sb.toString();
    }

    // ==================== Java 模板 ====================

    private static String generateJava(TestCase tc) {
        String className = sanitizeName(tc.getCaseName());
        StringBuilder sb = new StringBuilder();
        sb.append("/**\n");
        sb.append(" * 测试类: ").append(tc.getCaseName()).append("\n");
        sb.append(" * 用例编号: ").append(tc.getCasesID()).append("\n");
        sb.append(" * 优先级: ").append(tc.getPriority()).append("\n");
        sb.append(" * 超时: ").append(getTimeout(tc)).append("秒 | 重试: ").append(getRetryCount(tc)).append("次\n");
        if (hasTags(tc)) {
            sb.append(" * 标签: ").append(joinTags(tc)).append("\n");
        }
        sb.append(" */\n");
        sb.append("public class Test").append(className).append(" {\n\n");

        // 元数据常量
        sb.append("    // ========== 用例元数据 ==========\n");
        sb.append("    public static final String CASE_ID = \"").append(escape(tc.getCasesID())).append("\";\n");
        sb.append("    public static final String CASE_NAME = \"").append(escape(tc.getCaseName())).append("\";\n");
        sb.append("    public static final String PRIORITY = \"").append(escape(tc.getPriority())).append("\";\n");
        sb.append("    public static final int TIMEOUT = ").append(getTimeout(tc)).append("; // 秒\n");
        sb.append("    public static final int RETRY_COUNT = ").append(getRetryCount(tc)).append(";\n");
        if (hasTags(tc)) {
            sb.append("    public static final String[] TAGS = {");
            String[] tags = getTagArray(tc);
            for (int i = 0; i < tags.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(escape(tags[i])).append("\"");
            }
            sb.append("};\n");
        }
        sb.append("\n");

        // 前置条件
        sb.append("    // ========== 前置条件 ==========\n");
        String precond = getPrecondition(tc);
        if (precond != null && !precond.isEmpty()) {
            sb.append("    /**\n");
            sb.append("     * ").append(precond).append("\n");
            sb.append("     */\n");
            sb.append("    public static void setupPrecondition() {\n");
            sb.append("        // TODO: 实现前置条件\n");
            sb.append("    }\n\n");
        } else {
            sb.append("    // 无前置条件\n\n");
        }

        // 测试步骤
        sb.append("    // ========== 测试步骤 ==========\n");
        String[] steps = getSteps(tc);
        for (int i = 0; i < steps.length; i++) {
            sb.append("    /**\n");
            sb.append("     * 步骤").append(i + 1).append(": ").append(steps[i]).append("\n");
            sb.append("     */\n");
            sb.append("    public static void step").append(i + 1).append("() {\n");
            sb.append("        // TODO: 实现步骤").append(i + 1).append("\n");
            sb.append("    }\n\n");
        }

        // 预期结果
        sb.append("    // ========== 预期结果 ==========\n");
        String expectedDesc = getExpectedDescription(tc);
        sb.append("    public static final String EXPECTED_RESULT = \"\"\"\n").append(expectedDesc != null ? expectedDesc : "").append("\"\"\";\n");
        String[] checkpoints = getCheckpoints(tc);
        if (checkpoints != null && checkpoints.length > 0) {
            sb.append("    public static final String[] CHECKPOINTS = {\n");
            for (String cp : checkpoints) {
                sb.append("        \"").append(escape(cp)).append("\",\n");
            }
            sb.append("    };\n");
        }
        sb.append("\n");

        // 执行方法
        sb.append("    // ========== 测试执行 ==========\n");
        sb.append("    public static void execute() {\n");
        sb.append("        long startTime = System.currentTimeMillis();\n");
        sb.append("        try {\n");
        if (precond != null && !precond.isEmpty()) {
            sb.append("            setupPrecondition();\n");
        }
        for (int i = 0; i < steps.length; i++) {
            sb.append("            step").append(i + 1).append("();\n");
        }
        sb.append("        } finally {\n");
        sb.append("            long elapsed = System.currentTimeMillis() - startTime;\n");
        sb.append("            System.out.println(\"[执行耗时] \" + elapsed + \"ms\");\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        sb.append("    public static void main(String[] args) {\n");
        sb.append("        execute();\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    // ==================== C++ 模板 ====================

    private static String generateCpp(TestCase tc) {
        StringBuilder sb = new StringBuilder();
        sb.append("/**\n");
        sb.append(" * 测试文件: ").append(tc.getCaseName()).append("\n");
        sb.append(" * 用例编号: ").append(tc.getCasesID()).append("\n");
        sb.append(" * 优先级: ").append(tc.getPriority()).append("\n");
        sb.append(" */\n\n");
        sb.append("#include <iostream>\n");
        sb.append("#include <string>\n");
        sb.append("#include <vector>\n");
        sb.append("#include <chrono>\n\n");

        // 元数据
        sb.append("// ========== 用例元数据 ==========\n");
        sb.append("const std::string CASE_ID = \"").append(escape(tc.getCasesID())).append("\";\n");
        sb.append("const std::string CASE_NAME = \"").append(escape(tc.getCaseName())).append("\";\n");
        sb.append("const std::string PRIORITY = \"").append(escape(tc.getPriority())).append("\";\n");
        sb.append("const int TIMEOUT = ").append(getTimeout(tc)).append(";\n");
        sb.append("const int RETRY_COUNT = ").append(getRetryCount(tc)).append(";\n");
        sb.append("\n");

        // 前置条件
        sb.append("// ========== 前置条件 ==========\n");
        String precond = getPrecondition(tc);
        if (precond != null && !precond.isEmpty()) {
            sb.append("void setupPrecondition() {\n");
            sb.append("    // ").append(precond).append("\n");
            sb.append("    // TODO: 实现前置条件\n");
            sb.append("}\n\n");
        } else {
            sb.append("// 无前置条件\n\n");
        }

        // 步骤
        sb.append("// ========== 测试步骤 ==========\n");
        String[] steps = getSteps(tc);
        for (int i = 0; i < steps.length; i++) {
            sb.append("void step").append(i + 1).append("() {\n");
            sb.append("    // 步骤").append(i + 1).append(": ").append(steps[i]).append("\n");
            sb.append("    // TODO: 实现\n");
            sb.append("}\n\n");
        }

        // 预期结果
        sb.append("// ========== 预期结果 ==========\n");
        String expectedDesc = getExpectedDescription(tc);
        sb.append("const std::string EXPECTED_RESULT = R\"(\n").append(expectedDesc != null ? expectedDesc : "").append(")\";\n");
        sb.append("\n");

        // 主函数
        sb.append("// ========== 测试执行 ==========\n");
        sb.append("int main() {\n");
        sb.append("    auto start = std::chrono::steady_clock::now();\n");
        if (precond != null && !precond.isEmpty()) {
            sb.append("    setupPrecondition();\n");
        }
        for (int i = 0; i < steps.length; i++) {
            sb.append("    step").append(i + 1).append("();\n");
        }
        sb.append("    auto end = std::chrono::steady_clock::now();\n");
        sb.append("    auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);\n");
        sb.append("    std::cout << \"[执行耗时] \" << elapsed.count() << \"ms\" << std::endl;\n");
        sb.append("    return 0;\n");
        sb.append("}\n");

        return sb.toString();
    }

    // ==================== C# 模板 ====================

    private static String generateCSharp(TestCase tc) {
        String className = sanitizeName(tc.getCaseName());
        StringBuilder sb = new StringBuilder();
        sb.append("/**\n");
        sb.append(" * 测试类: ").append(tc.getCaseName()).append("\n");
        sb.append(" * 用例编号: ").append(tc.getCasesID()).append("\n");
        sb.append(" * 优先级: ").append(tc.getPriority()).append("\n");
        sb.append(" */\n");
        sb.append("using System;\n");
        sb.append("using System.Diagnostics;\n");
        sb.append("using System.Collections.Generic;\n\n");
        sb.append("namespace AutoTest {\n");
        sb.append("public class Test").append(className).append(" {\n\n");

        // 元数据
        sb.append("    // ========== 用例元数据 ==========\n");
        sb.append("    public const string CaseId = \"").append(escape(tc.getCasesID())).append("\";\n");
        sb.append("    public const string CaseName = \"").append(escape(tc.getCaseName())).append("\";\n");
        sb.append("    public const string Priority = \"").append(escape(tc.getPriority())).append("\";\n");
        sb.append("    public const int Timeout = ").append(getTimeout(tc)).append(";\n");
        sb.append("    public const int RetryCount = ").append(getRetryCount(tc)).append(";\n");
        if (hasTags(tc)) {
            sb.append("    public static readonly string[] Tags = { ");
            String[] tags = getTagArray(tc);
            for (int i = 0; i < tags.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(escape(tags[i])).append("\"");
            }
            sb.append(" };\n");
        }
        sb.append("\n");

        // 前置条件
        sb.append("    // ========== 前置条件 ==========\n");
        String precond = getPrecondition(tc);
        if (precond != null && !precond.isEmpty()) {
            sb.append("    /// <summary>").append(precond).append("</summary>\n");
            sb.append("    public static void SetupPrecondition() {\n");
            sb.append("        // TODO: 实现前置条件\n");
            sb.append("    }\n\n");
        }

        // 步骤
        sb.append("    // ========== 测试步骤 ==========\n");
        String[] steps = getSteps(tc);
        for (int i = 0; i < steps.length; i++) {
            sb.append("    /// <summary>步骤").append(i + 1).append(": ").append(steps[i]).append("</summary>\n");
            sb.append("    public static void Step").append(i + 1).append("() {\n");
            sb.append("        // TODO: 实现步骤").append(i + 1).append("\n");
            sb.append("    }\n\n");
        }

        // 预期结果
        sb.append("    // ========== 预期结果 ==========\n");
        String expectedDesc = getExpectedDescription(tc);
        sb.append("    public const string ExpectedResult = @\"\n").append(expectedDesc != null ? expectedDesc : "").append("\";\n");
        sb.append("\n");

        // 执行
        sb.append("    // ========== 测试执行 ==========\n");
        sb.append("    public static void Execute() {\n");
        sb.append("        var sw = Stopwatch.StartNew();\n");
        if (precond != null && !precond.isEmpty()) {
            sb.append("        SetupPrecondition();\n");
        }
        for (int i = 0; i < steps.length; i++) {
            sb.append("        Step").append(i + 1).append("();\n");
        }
        sb.append("        sw.Stop();\n");
        sb.append("        Console.WriteLine($\"[执行耗时] {sw.ElapsedMilliseconds}ms\");\n");
        sb.append("    }\n\n");

        sb.append("    public static void Main(string[] args) {\n");
        sb.append("        Execute();\n");
        sb.append("    }\n");
        sb.append("}\n");
        sb.append("}\n");

        return sb.toString();
    }

    // ==================== GDScript 模板 ====================

    private static String generateGdscript(TestCase tc) {
        StringBuilder sb = new StringBuilder();
        sb.append("# @tool\n");
        sb.append("# @icon(\"res://icons/test.png\")\n");
        sb.append("## 测试脚本: ").append(tc.getCaseName()).append("\n");
        sb.append("## 用例编号: ").append(tc.getCasesID()).append("\n");
        sb.append("## 优先级: ").append(tc.getPriority()).append("\n");
        sb.append("## 超时: ").append(getTimeout(tc)).append("秒 | 重试: ").append(getRetryCount(tc)).append("次\n");
        if (hasTags(tc)) {
            sb.append("## 标签: ").append(joinTags(tc)).append("\n");
        }
        sb.append("\nextends Node\n\n");

        // 元数据
        sb.append("# ========== 用例元数据 ==========\n");
        sb.append("var case_id: String = \"").append(escape(tc.getCasesID())).append("\"\n");
        sb.append("var case_name: String = \"").append(escape(tc.getCaseName())).append("\"\n");
        sb.append("var priority: String = \"").append(escape(tc.getPriority())).append("\"\n");
        sb.append("var timeout_sec: int = ").append(getTimeout(tc)).append("\n");
        sb.append("var retry_count: int = ").append(getRetryCount(tc)).append("\n");
        if (hasTags(tc)) {
            sb.append("var tags: Array[String] = [");
            String[] tags = getTagArray(tc);
            for (int i = 0; i < tags.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(escape(tags[i])).append("\"");
            }
            sb.append("]\n");
        }
        sb.append("\n");

        // 前置条件
        sb.append("# ========== 前置条件 ==========\n");
        String precond = getPrecondition(tc);
        if (precond != null && !precond.isEmpty()) {
            sb.append("## ").append(precond).append("\n");
            sb.append("func _setup_precondition() -> void:\n");
            sb.append("\t# TODO: 实现前置条件\n");
            sb.append("\tpass\n\n");
        }

        // 步骤
        sb.append("# ========== 测试步骤 ==========\n");
        String[] steps = getSteps(tc);
        for (int i = 0; i < steps.length; i++) {
            sb.append("## 步骤").append(i + 1).append(": ").append(steps[i]).append("\n");
            sb.append("func _step_").append(i + 1).append("() -> void:\n");
            sb.append("\t# TODO: 实现步骤").append(i + 1).append("\n");
            sb.append("\tpass\n\n");
        }

        // 预期结果
        sb.append("# ========== 预期结果 ==========\n");
        String expectedDesc = getExpectedDescription(tc);
        sb.append("var expected_result: String = \"\"\"\n").append(expectedDesc != null ? expectedDesc : "").append("\"\"\"\n");
        sb.append("\n");

        // 执行
        sb.append("# ========== 测试执行 ==========\n");
        sb.append("func _ready() -> void:\n");
        sb.append("\tvar start_time_msec := Time.get_ticks_msec()\n");
        if (precond != null && !precond.isEmpty()) {
            sb.append("\t_setup_precondition()\n");
        }
        for (int i = 0; i < steps.length; i++) {
            sb.append("\t_step_").append(i + 1).append("()\n");
        }
        sb.append("\tvar elapsed := Time.get_ticks_msec() - start_time_msec\n");
        sb.append("\tprint(\"[执行耗时] %dms\" % elapsed)\n");

        return sb.toString();
    }

    // ==================== 独立（不关联用例）模板 ====================

    private static String generateStandalonePython() {
        return "#!/usr/bin/env python3\n" +
                "# -*- coding: utf-8 -*-\n" +
                "\"\"\"\n" +
                "测试脚本: (独立脚本，未关联用例)\n" +
                "优先级:   \n" +
                "超时:     30秒\n" +
                "重试:     1次\n" +
                "\"\"\"\n\n" +
                "import unittest\n" +
                "import time\n\n" +
                "# ========== 用例元数据 ==========\n" +
                "CASE_ID = \"\"\n" +
                "CASE_NAME = \"\"          # <-- 请填写用例名称（填写后保存时可自动命名文件）\n" +
                "PRIORITY = \"\"\n" +
                "TIMEOUT = 30\n" +
                "RETRY_COUNT = 1\n\n" +
                "# ========== 前置条件 ==========\n" +
                "def setup_precondition():\n" +
                "    \"\"\"前置条件\"\"\"\n" +
                "    # TODO: 实现前置条件\n" +
                "    pass\n\n" +
                "# ========== 测试步骤 ==========\n" +
                "def step_1():\n" +
                "    \"\"\"步骤1: (请描述)\"\"\"\n" +
                "    # TODO: 实现步骤1\n" +
                "    pass\n\n" +
                "# ========== 预期结果 ==========\n" +
                "EXPECTED_RESULT = \"\"\"(请描述预期结果)\"\"\"\n\n" +
                "# ========== 测试执行 ==========\n" +
                "class TestTestCase(unittest.TestCase):\n\n" +
                "    def setUp(self):\n" +
                "        self.start_time = time.time()\n" +
                "        setup_precondition()\n\n" +
                "    def test_case(self):\n" +
                "        \"\"\"执行测试\"\"\"\n" +
                "        step_1()\n\n" +
                "    def tearDown(self):\n" +
                "        elapsed = time.time() - self.start_time\n" +
                "        print(f\"[执行耗时] {elapsed:.2f}秒\")\n\n" +
                "if __name__ == \"__main__\":\n" +
                "    unittest.main()\n";
    }

    private static String generateStandaloneJava() {
        return "/**\n" +
                " * 测试类: (独立脚本，未关联用例)\n" +
                " * 优先级:   | 超时: 30秒 | 重试: 1次\n" +
                " */\n" +
                "public class TestTestCase {\n\n" +
                "    // ========== 用例元数据 ==========\n" +
                "    public static final String CASE_ID = \"\";\n" +
                "    public static final String CASE_NAME = \"\";     // <-- 请填写用例名称\n" +
                "    public static final String PRIORITY = \"\";\n" +
                "    public static final int TIMEOUT = 30; // 秒\n" +
                "    public static final int RETRY_COUNT = 1;\n\n" +
                "    // ========== 前置条件 ==========\n" +
                "    public static void setupPrecondition() {\n" +
                "        // TODO: 实现前置条件\n" +
                "    }\n\n" +
                "    // ========== 测试步骤 ==========\n" +
                "    public static void step1() {\n" +
                "        // 步骤1: (请描述)\n" +
                "        // TODO: 实现\n" +
                "    }\n\n" +
                "    // ========== 预期结果 ==========\n" +
                "    public static final String EXPECTED_RESULT = \"(请描述预期结果)\";\n\n" +
                "}\n";
    }

    private static String generateStandaloneCpp() {
        return "/**\n" +
                " * 测试文件: (独立脚本，未关联用例)\n" +
                " * 优先级:\n" +
                " */\n\n" +
                "#include <iostream>\n" +
                "#include <string>\n" +
                "#include <vector>\n" +
                "#include <chrono>\n\n" +
                "// ========== 用例元数据 ==========\n" +
                "const std::string CASE_ID = \"\";\n" +
                "const std::string CASE_NAME = \"\";      // <-- 请填写用例名称\n" +
                "const std::string PRIORITY = \"\";\n" +
                "const int TIMEOUT = 30;\n" +
                "const int RETRY_COUNT = 1;\n\n" +
                "// ========== 前置条件 ==========\n" +
                "void setupPrecondition() {\n" +
                "    // TODO: 实现前置条件\n" +
                "}\n\n" +
                "// ========== 测试步骤 ==========\n" +
                "void step1() {\n" +
                "    // 步骤1: (请描述)\n" +
                "    // TODO: 实现\n" +
                "}\n\n" +
                "// ========== 预期结果 ==========\n" +
                "const std::string EXPECTED_RESULT = \"(请描述预期结果)\";\n";
    }

    private static String generateStandaloneCSharp() {
        return "/**\n" +
                " * 测试类: (独立脚本，未关联用例)\n" +
                " * 优先级:\n" +
                " */\n" +
                "using System;\n" +
                "using System.Diagnostics;\n" +
                "using System.Collections.Generic;\n\n" +
                "namespace AutoTest {\n" +
                "public class TestTestCase {\n\n" +
                "    // ========== 用例元数据 ==========\n" +
                "    public const string CaseId = \"\";\n" +
                "    public const string CaseName = \"\";     // <-- 请填写用例名称\n" +
                "    public const string Priority = \"\";\n" +
                "    public const int Timeout = 30;\n" +
                "    public const int RetryCount = 1;\n\n" +
                "    // ========== 前置条件 ==========\n" +
                "    public static void SetupPrecondition() {\n" +
                "        // TODO: 实现前置条件\n" +
                "    }\n\n" +
                "    // ========== 测试步骤 ==========\n" +
                "    public static void Step1() {\n" +
                "        // 步骤1: (请描述)\n" +
                "        // TODO: 实现\n" +
                "    }\n\n" +
                "    // ========== 预期结果 ==========\n" +
                "    public const string ExpectedResult = \"(请描述预期结果)\";\n" +
                "}\n" +
                "}\n";
    }

    private static String generateStandaloneGdscript() {
        return "# @tool\n" +
                "# @icon(\"res://icons/test.png\")\n" +
                "## 测试脚本: (独立脚本，未关联用例)\n" +
                "## 优先级:   | 超时: 30秒 | 重试: 1次\n" +
                "\nextends Node\n\n" +
                "# ========== 用例元数据 ==========\n" +
                "var case_id: String = \"\"\n" +
                "var case_name: String = \"\"       # <-- 请填写用例名称\n" +
                "var priority: String = \"\"\n" +
                "var timeout_sec: int = 30\n" +
                "var retry_count: int = 1\n\n" +
                "# ========== 前置条件 ==========\n" +
                "## 前置条件\n" +
                "func _setup_precondition() -> void:\n" +
                "\t# TODO: 实现前置条件\n" +
                "\tpass\n\n" +
                "# ========== 测试步骤 ==========\n" +
                "## 步骤1: (请描述)\n" +
                "func _step_1() -> void:\n" +
                "\t# TODO: 实现步骤1\n" +
                "\tpass\n\n" +
                "# ========== 预期结果 ==========\n" +
                "var expected_result: String = \"\"\"(请描述预期结果)\"\"\"\n\n" +
                "# ========== 测试执行 ==========\n" +
                "func _ready() -> void:\n" +
                "\tvar start_time_msec := Time.get_ticks_msec()\n" +
                "\t_setup_precondition()\n" +
                "\t_step_1()\n" +
                "\tvar elapsed := Time.get_ticks_msec() - start_time_msec\n" +
                "\tprint(\"[执行耗时] %dms\" % elapsed)\n";
    }

    // ==================== 工具方法 ====================

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isEmpty()) return "TestCase";
        // 移除非字母数字字符，保留下划线
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            } else {
                nextUpper = true;
            }
        }
        String result = sb.toString();
        return result.isEmpty() ? "TestCase" : result;
    }

    private static int getTimeout(TestCase tc) {
        return tc.getExecution() != null && tc.getExecution().getTimeout() != null
                ? tc.getExecution().getTimeout() : 30;
    }

    private static int getRetryCount(TestCase tc) {
        return tc.getExecution() != null && tc.getExecution().getRetryCount() != null
                ? tc.getExecution().getRetryCount() : 1;
    }

    private static boolean hasTags(TestCase tc) {
        return tc.getExecution() != null && tc.getExecution().getTags() != null
                && !tc.getExecution().getTags().isEmpty();
    }

    private static String joinTags(TestCase tc) {
        return String.join(", ", tc.getExecution().getTags());
    }

    private static String[] getTagArray(TestCase tc) {
        return tc.getExecution().getTags().toArray(new String[0]);
    }

    private static String getPrecondition(TestCase tc) {
        return tc.getCaseInfo() != null ? tc.getCaseInfo().getPrecondition() : null;
    }

    private static String[] getSteps(TestCase tc) {
        if (tc.getCaseInfo() != null && tc.getCaseInfo().getStepsToReproduce() != null) {
            return tc.getCaseInfo().getStepsToReproduce().toArray(new String[0]);
        }
        return new String[]{"(未定义步骤)"};
    }

    private static String getExpectedDescription(TestCase tc) {
        return tc.getExpectedResult() != null ? tc.getExpectedResult().getDescription() : null;
    }

    private static String[] getCheckpoints(TestCase tc) {
        if (tc.getExpectedResult() != null && tc.getExpectedResult().getCheckpoints() != null) {
            return tc.getExpectedResult().getCheckpoints().toArray(new String[0]);
        }
        return null;
    }
}

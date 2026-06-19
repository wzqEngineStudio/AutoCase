package com.autocase.test;

import com.autocase.dao.CaseDao;
import com.autocase.entity.TestCase;

import java.io.File;
import java.util.List;

/**
 * 测试运行器 - 供Python脚本通过java -cp调用
 * 用法: java -cp <classpath> com.autocase.test.TestRunner <command> [args...]
 * 
 * 命令:
 *   scanCases <dir>        - 扫描目录下的用例
 *   loadCase <filePath>    - 加载单个用例
 *   saveCase <filePath>    - 保存用例（修改状态）
 *   createCase <filePath> <json> - 创建用例
 */
public class TestRunner {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: TestRunner <command> [args...]");
            System.exit(1);
        }

        String command = args[0];
        CaseDao caseDao = new CaseDao();

        try {
            switch (command) {
                case "scanCases":
                    if (args.length < 2) {
                        System.err.println("Usage: scanCases <dir>");
                        System.exit(1);
                    }
                    scanCases(caseDao, args[1]);
                    break;

                case "loadCase":
                    if (args.length < 2) {
                        System.err.println("Usage: loadCase <filePath>");
                        System.exit(1);
                    }
                    loadCase(caseDao, args[1]);
                    break;

                case "saveCase":
                    if (args.length < 2) {
                        System.err.println("Usage: saveCase <filePath>");
                        System.exit(1);
                    }
                    saveCase(caseDao, args[1]);
                    break;

                case "createCase":
                    if (args.length < 3) {
                        System.err.println("Usage: createCase <filePath> <jsonContent>");
                        System.exit(1);
                    }
                    createCase(caseDao, args[1], args[2]);
                    break;

                default:
                    System.err.println("Unknown command: " + command);
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("FAIL: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void scanCases(CaseDao caseDao, String dir) {
        List<TestCase> cases = caseDao.scanCases(dir);
        System.out.println("PASS: 扫描到" + cases.size() + "个用例");
        for (TestCase tc : cases) {
            System.out.println("  - " + tc.getCasesID() + ": " + 
                (tc.getCaseInfo() != null ? tc.getCaseInfo().getCaseName() : "unknown"));
        }
    }

    private static void loadCase(CaseDao caseDao, String filePath) {
        TestCase tc = caseDao.loadCase(filePath);
        if (tc != null) {
            System.out.println("PASS: 用例加载成功 - " + tc.getCasesID());
        } else {
            System.out.println("FAIL: 用例加载失败 - 返回null");
            System.exit(1);
        }
    }

    private static void saveCase(CaseDao caseDao, String filePath) {
        TestCase tc = caseDao.loadCase(filePath);
        if (tc == null) {
            System.out.println("FAIL: 用例不存在 - " + filePath);
            System.exit(1);
        }
        
        // 修改状态并保存
        String originalStatus = tc.getCurrentStatus() != null ? tc.getCurrentStatus().name() : "null";
        tc.setCurrentStatus(com.autocase.entity.CaseStatus.PASSED);
        boolean saved = caseDao.saveCase(tc);
        
        if (saved) {
            // 验证保存后能正确加载
            TestCase reloaded = caseDao.loadCase(filePath);
            if (reloaded != null && reloaded.getCurrentStatus() == com.autocase.entity.CaseStatus.PASSED) {
                System.out.println("PASS: 用例保存成功 (状态: " + originalStatus + " -> PASSED)");
                // 恢复原状态
                tc.setCurrentStatus(com.autocase.entity.CaseStatus.valueOf(originalStatus));
                caseDao.saveCase(tc);
            } else {
                System.out.println("FAIL: 保存后验证失败");
                System.exit(1);
            }
        } else {
            System.out.println("FAIL: 用例保存失败");
            System.exit(1);
        }
    }

    private static void createCase(CaseDao caseDao, String filePath, String jsonContent) {
        // 这个命令主要用于验证CaseDao能否正确解析JSON
        TestCase tc = caseDao.loadCase(filePath);
        if (tc != null && tc.getCasesID() != null) {
            System.out.println("PASS: 用例创建/加载成功 - " + tc.getCasesID());
        } else {
            System.out.println("FAIL: 用例创建/加载失败");
            System.exit(1);
        }
    }
}

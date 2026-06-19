package com.autocase.logic;

import com.autocase.dao.CaseDao;
import com.autocase.dao.ConfigDao;
import com.autocase.entity.CaseStatus;
import com.autocase.entity.GlobalConfig;
import com.autocase.entity.TestCase;
import com.autocase.util.HashCache;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用例业务逻辑类
 */
public class CaseLogic {

    private final CaseDao caseDao;
    private final ConfigDao configDao;
    private List<TestCase> allCases = new ArrayList<>();

    public CaseLogic() {
        caseDao = new CaseDao();
        configDao = new ConfigDao();
    }

    /**
     * 加载指定目录下的所有用例
     * 使用增量缓存：根目录不变时，仅文件变动才触发重扫
     *
     * @param directoryPath 目录路径
     */
    @SuppressWarnings("unchecked")
    public void loadCases(String directoryPath) {
        HashCache cache = HashCache.getInstance();

        // 检查缓存状态（基于脏标记，零遍历设计）
        HashCache.CacheResult<Object> result =
                cache.getOrCheck(directoryPath, HashCache.CacheType.CASES);

        if (result.isFresh()) {
            // 缓存完全命中 → 零扫描，直接使用
            allCases = (List<TestCase>) result.getData();
            return;
        }

        // 缓存过期 → 执行全量扫描
        allCases = caseDao.scanCases(directoryPath);

        // 写入缓存（异步持久化，更新两级哈希）
        cache.put(directoryPath, HashCache.CacheType.CASES, new ArrayList<>(allCases));
    }

    /**
     * 获取所有用例
     */
    public List<TestCase> getAllCases() {
        return new ArrayList<>(allCases);
    }

    /**
     * 按名称模糊匹配筛选
     * @param keyword 关键词
     * @return 匹配的用例列表
     */
    public List<TestCase> filterByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCases();
        }

        String lowerKeyword = keyword.toLowerCase();
        return allCases.stream()
                .filter(c -> c.getCaseName() != null && c.getCaseName().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }

    /**
     * 按编号范围筛选
     * @param startId 起始编号（包含）
     * @param endId 结束编号（包含）
     * @return 范围内的用例列表
     */
    public List<TestCase> filterByIdRange(int startId, int endId) {
        return allCases.stream()
                .filter(c -> {
                    String id = c.getCasesID();
                    if (id == null) return false;
                    // 提取编号中的数字部分
                    String numStr = id.replaceAll("[^0-9]", "");
                    if (numStr.isEmpty()) return false;
                    try {
                        int num = Integer.parseInt(numStr);
                        return num >= startId && num <= endId;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 按状态筛选
     * @param status 用例状态
     * @return 匹配状态的用例列表
     */
    public List<TestCase> filterByStatus(CaseStatus status) {
        if (status == null) {
            return getAllCases();
        }

        return allCases.stream()
                .filter(c -> c.getCurrentStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * 按严重程度筛选
     * @param severity 严重程度
     * @return 匹配严重程度的用例列表
     */
    public List<TestCase> filterBySeverity(String severity) {
        if (severity == null || severity.isEmpty()) {
            return getAllCases();
        }

        return allCases.stream()
                .filter(c -> severity.equals(c.getSeverity()))
                .collect(Collectors.toList());
    }

    /**
     * 组合筛选
     * @param nameKeyword 名称关键词
     * @param startId 起始编号
     * @param endId 结束编号
     * @param status 状态
     * @param severity 严重程度
     * @return 筛选后的用例列表
     */
    public List<TestCase> filterCases(String nameKeyword, Integer startId, Integer endId, 
                                      CaseStatus status, String severity) {
        List<TestCase> result = new ArrayList<>(allCases);

        // 名称筛选
        if (nameKeyword != null && !nameKeyword.trim().isEmpty()) {
            String lowerKeyword = nameKeyword.toLowerCase();
            result = result.stream()
                    .filter(c -> c.getCaseName() != null && c.getCaseName().toLowerCase().contains(lowerKeyword))
                    .collect(Collectors.toList());
        }

        // 编号范围筛选
        if (startId != null && endId != null) {
            int start = startId;
            int end = endId;
            result = result.stream()
                    .filter(c -> {
                        String id = c.getCasesID();
                        if (id == null) return false;
                        String numStr = id.replaceAll("[^0-9]", "");
                        if (numStr.isEmpty()) return false;
                        try {
                            int num = Integer.parseInt(numStr);
                            return num >= start && num <= end;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        }

        // 状态筛选
        if (status != null) {
            result = result.stream()
                    .filter(c -> c.getCurrentStatus() == status)
                    .collect(Collectors.toList());
        }

        // 严重程度筛选
        if (severity != null && !severity.isEmpty()) {
            result = result.stream()
                    .filter(c -> severity.equals(c.getSeverity()))
                    .collect(Collectors.toList());
        }

        return result;
    }

    /**
     * 更新用例状态并保存
     * @param testCase 用例
     * @param newStatus 新状态
     * @return 是否成功
     */
    public boolean updateCaseStatus(TestCase testCase, CaseStatus newStatus) {
        if (testCase == null) {
            return false;
        }

        testCase.setCurrentStatus(newStatus);
        return caseDao.saveCase(testCase);
    }

    /**
     * 创建新用例（使用全局配置的格式）
     * @param directoryPath 目录路径
     * @param testCase 用例实体
     * @return 是否成功
     */
    public boolean createCase(String directoryPath, TestCase testCase) {
        GlobalConfig globalConfig = configDao.getGlobalConfig();
        String format = globalConfig.getCaseFormat();
        boolean success = caseDao.createCase(directoryPath, testCase, format);
        if (success) {
            allCases.add(testCase);
        }
        return success;
    }

    /**
     * 删除用例
     * @param testCase 用例
     * @return 是否成功
     */
    public boolean deleteCase(TestCase testCase) {
        boolean success = caseDao.deleteCase(testCase);
        if (success) {
            allCases.remove(testCase);
        }
        return success;
    }

    /**
     * 保存用例修改
     * @param testCase 用例
     * @return 是否成功
     */
    public boolean saveCase(TestCase testCase) {
        return caseDao.saveCase(testCase);
    }

    /**
     * 按优先级筛选
     * @param priority 优先级
     * @return 匹配优先级的用例列表
     */
    public List<TestCase> filterByPriority(String priority) {
        if (priority == null || priority.isEmpty()) {
            return getAllCases();
        }

        return allCases.stream()
                .filter(c -> priority.equals(c.getPriority()))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有唯一的严重程度
     */
    public List<String> getUniqueSeverities() {
        return allCases.stream()
                .map(TestCase::getSeverity)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 获取所有唯一的优先级
     */
    public List<String> getUniquePriorities() {
        return allCases.stream()
                .map(TestCase::getPriority)
                .filter(p -> p != null && !p.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 获取所有唯一的分组
     */
    public List<String> getUniqueGroups() {
        return allCases.stream()
                .map(TestCase::getGroup)
                .filter(g -> g != null && !g.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}

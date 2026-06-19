package com.autocase.dao;

import com.autocase.dao.mapper.ManualTestBatchMapper;
import com.autocase.dao.mapper.ManualTestDetailMapper;
import com.autocase.dao.mapper.TaskGroupMapper;
import com.autocase.entity.ManualTestBatch;
import com.autocase.entity.ManualTestDetail;
import com.autocase.entity.TaskGroup;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 手工测试 DAO - 使用 MyBatis 管理 H2 数据库
 * 数据模型: TaskGroup(任务组) → ManualTestBatch(任务) → ManualTestDetail(用例)
 */
public class ManualTestDao {

    private final SqlSessionFactory sqlSessionFactory;

    public ManualTestDao() {
        this.sqlSessionFactory = buildSqlSessionFactory();
        initDatabase();
    }

    private SqlSessionFactory buildSqlSessionFactory() {
        try {
            String resource = "mybatis-config.xml";
            Reader reader = Resources.getResourceAsReader(resource);
            return new SqlSessionFactoryBuilder().build(reader);
        } catch (IOException e) {
            throw new RuntimeException("加载 MyBatis 配置失败", e);
        }
    }

    /**
     * 初始化数据库表（含平滑升级）
     */
    private void initDatabase() {
        String createGroupSql = """
            CREATE TABLE IF NOT EXISTS task_group (
                group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                group_name VARCHAR(100),
                description VARCHAR(500),
                create_time TIMESTAMP,
                remark VARCHAR(1000)
            )
            """;

        String createBatchSql = """
            CREATE TABLE IF NOT EXISTS manual_test_batch (
                batch_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                group_id BIGINT,
                task_name VARCHAR(100),
                version VARCHAR(50),
                module_name VARCHAR(50),
                tester VARCHAR(50),
                total_cases INT,
                passed_count INT,
                failed_count INT,
                blocked_count INT,
                pending_count INT,
                start_time TIMESTAMP,
                end_time TIMESTAMP,
                remark VARCHAR(1000)
            )
            """;

        String createDetailSql = """
            CREATE TABLE IF NOT EXISTS manual_test_detail (
                detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                batch_id BIGINT,
                case_id VARCHAR(50),
                case_name VARCHAR(100),
                status VARCHAR(20),
                expected_result VARCHAR(2000),
                actual_result VARCHAR(2000),
                failure_reason VARCHAR(2000),
                attachment_path VARCHAR(500),
                remark VARCHAR(1000),
                linked_auto_case_id VARCHAR(50),
                priority VARCHAR(20),
                severity VARCHAR(20),
                execution_time TIMESTAMP
            )
            """;

        // 兼容性：为已存在的表添加新列
        String alterBatchGroupId = "ALTER TABLE manual_test_batch ADD COLUMN IF NOT EXISTS group_id BIGINT";
        String alterDetailPriority = "ALTER TABLE manual_test_detail ADD COLUMN IF NOT EXISTS priority VARCHAR(20)";
        String alterDetailSeverity = "ALTER TABLE manual_test_detail ADD COLUMN IF NOT EXISTS severity VARCHAR(20)";

        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createGroupSql);
            stmt.execute(createBatchSql);
            stmt.execute(createDetailSql);
            try { stmt.execute(alterBatchGroupId); } catch (SQLException ignored) {}
            try { stmt.execute(alterDetailPriority); } catch (SQLException ignored) {}
            try { stmt.execute(alterDetailSeverity); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            System.err.println("初始化手工测试数据库失败: " + e.getMessage());
        }
    }

    // ==================== 任务组 CRUD ====================

    public Long insertTaskGroup(TaskGroup group) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TaskGroupMapper mapper = session.getMapper(TaskGroupMapper.class);
            mapper.insert(group);
            session.commit();
            return group.getGroupId();
        } catch (Exception e) {
            System.err.println("插入任务组失败: " + e.getMessage());
            return null;
        }
    }

    public List<TaskGroup> getAllTaskGroups() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TaskGroupMapper mapper = session.getMapper(TaskGroupMapper.class);
            return mapper.selectAll();
        } catch (Exception e) {
            System.err.println("查询任务组失败: " + e.getMessage());
            return List.of();
        }
    }

    public TaskGroup getTaskGroupById(Long groupId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TaskGroupMapper mapper = session.getMapper(TaskGroupMapper.class);
            return mapper.selectById(groupId);
        } catch (Exception e) {
            System.err.println("查询任务组失败: " + e.getMessage());
            return null;
        }
    }

    public TaskGroup getTaskGroupByName(String groupName) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TaskGroupMapper mapper = session.getMapper(TaskGroupMapper.class);
            return mapper.selectByName(groupName);
        } catch (Exception e) {
            return null;
        }
    }

    public void updateTaskGroup(TaskGroup group) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TaskGroupMapper mapper = session.getMapper(TaskGroupMapper.class);
            mapper.update(group);
            session.commit();
        } catch (Exception e) {
            System.err.println("更新任务组失败: " + e.getMessage());
        }
    }

    public void deleteTaskGroup(Long groupId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ManualTestBatchMapper batchMapper = session.getMapper(ManualTestBatchMapper.class);
            ManualTestDetailMapper detailMapper = session.getMapper(ManualTestDetailMapper.class);
            TaskGroupMapper groupMapper = session.getMapper(TaskGroupMapper.class);

            // 先删除该组下所有任务的详情
            List<ManualTestBatch> batches = batchMapper.selectByGroupId(groupId);
            for (ManualTestBatch batch : batches) {
                detailMapper.deleteByBatchId(batch.getBatchId());
            }
            // 再删除该组下所有任务
            batchMapper.deleteByGroupId(groupId);
            // 最后删除任务组
            groupMapper.deleteById(groupId);

            session.commit();
        } catch (Exception e) {
            System.err.println("删除任务组失败: " + e.getMessage());
        }
    }

    // ==================== 任务(Batch) CRUD ====================

    public Long insertBatch(ManualTestBatch batch) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ManualTestBatchMapper mapper = session.getMapper(ManualTestBatchMapper.class);
            mapper.insert(batch);
            session.commit();
            return batch.getBatchId();
        } catch (Exception e) {
            System.err.println("插入手工测试批次失败: " + e.getMessage());
            return null;
        }
    }

    public void insertDetail(ManualTestDetail detail) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ManualTestDetailMapper mapper = session.getMapper(ManualTestDetailMapper.class);
            mapper.insert(detail);
            session.commit();
        } catch (Exception e) {
            System.err.println("插入手工测试详情失败: " + e.getMessage());
        }
    }

    public void updateDetail(ManualTestDetail detail) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ManualTestDetailMapper mapper = session.getMapper(ManualTestDetailMapper.class);
            mapper.update(detail);
            session.commit();
        } catch (Exception e) {
            System.err.println("更新手工测试详情失败: " + e.getMessage());
        }
    }

    public void updateBatch(ManualTestBatch batch) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ManualTestBatchMapper mapper = session.getMapper(ManualTestBatchMapper.class);
            mapper.update(batch);
            session.commit();
        } catch (Exception e) {
            System.err.println("更新手工测试批次失败: " + e.getMessage());
        }
    }

    public List<ManualTestBatch> getAllBatches() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ManualTestBatchMapper mapper = session.getMapper(ManualTestBatchMapper.class);
            return mapper.selectAll();
        } catch (Exception e) {
            System.err.println("查询手工测试批次失败: " + e.getMessage());
            return List.of();
        }
    }

    public List<ManualTestBatch> getBatchesByGroupId(Long groupId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ManualTestBatchMapper mapper = session.getMapper(ManualTestBatchMapper.class);
            return mapper.selectByGroupId(groupId);
        } catch (Exception e) {
            System.err.println("查询任务组下的任务失败: " + e.getMessage());
            return List.of();
        }
    }

    public List<ManualTestDetail> getDetailsByBatchId(Long batchId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ManualTestDetailMapper mapper = session.getMapper(ManualTestDetailMapper.class);
            return mapper.selectByBatchId(batchId);
        } catch (Exception e) {
            System.err.println("查询手工测试详情失败: " + e.getMessage());
            return List.of();
        }
    }

    public List<ManualTestDetail> getDetailsByCaseId(String caseId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ManualTestDetailMapper mapper = session.getMapper(ManualTestDetailMapper.class);
            return mapper.selectByCaseId(caseId);
        } catch (Exception e) {
            System.err.println("查询用例手工测试历史失败: " + e.getMessage());
            return List.of();
        }
    }

    public void deleteBatch(Long batchId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ManualTestDetailMapper detailMapper = session.getMapper(ManualTestDetailMapper.class);
            ManualTestBatchMapper batchMapper = session.getMapper(ManualTestBatchMapper.class);

            detailMapper.deleteByBatchId(batchId);
            batchMapper.deleteById(batchId);

            session.commit();
        } catch (Exception e) {
            System.err.println("删除手工测试批次失败: " + e.getMessage());
        }
    }

    public void clearAllHistory() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ManualTestDetailMapper detailMapper = session.getMapper(ManualTestDetailMapper.class);
            ManualTestBatchMapper batchMapper = session.getMapper(ManualTestBatchMapper.class);
            TaskGroupMapper groupMapper = session.getMapper(TaskGroupMapper.class);

            detailMapper.deleteAllDetails();
            batchMapper.deleteAll();
            groupMapper.deleteAll();

            session.commit();
        } catch (Exception e) {
            System.err.println("清除所有手工测试历史失败: " + e.getMessage());
        }
    }
}

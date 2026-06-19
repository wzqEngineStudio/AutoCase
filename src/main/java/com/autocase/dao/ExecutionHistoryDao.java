package com.autocase.dao;

import com.autocase.dao.mapper.ExecutionBatchMapper;
import com.autocase.dao.mapper.ExecutionDetailMapper;
import com.autocase.entity.ExecutionBatch;
import com.autocase.entity.ExecutionDetail;
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
 * 执行历史 DAO - 使用 MyBatis 管理 H2 数据库
 */
public class ExecutionHistoryDao {

    private final SqlSessionFactory sqlSessionFactory;

    public ExecutionHistoryDao() {
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
     * 初始化数据库表
     */
    private void initDatabase() {
        String createBatchSql = """
            CREATE TABLE IF NOT EXISTS execution_batch (
                batch_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                trigger_type VARCHAR(20),
                trigger_user VARCHAR(50),
                total_cases INT,
                passed_count INT,
                failed_count INT,
                blocked_count INT,
                start_time TIMESTAMP,
                end_time TIMESTAMP,
                git_commit VARCHAR(40)
            )
            """;

        String createDetailSql = """
            CREATE TABLE IF NOT EXISTS execution_detail (
                detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                batch_id BIGINT,
                case_id VARCHAR(50),
                status VARCHAR(20),
                duration_ms INT,
                failure_reason VARCHAR(2000),
                output_log VARCHAR(10000),
                stack_trace CLOB,
                execution_time TIMESTAMP,
                expected_result VARCHAR(2000),
                actual_result VARCHAR(2000)
            )
            """;

        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createBatchSql);
            stmt.execute(createDetailSql);
        } catch (SQLException e) {
            System.err.println("初始化执行历史数据库失败: " + e.getMessage());
        }
    }

    /**
     * 插入执行批次，返回自增主键
     */
    public Long insertBatch(ExecutionBatch batch) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ExecutionBatchMapper mapper = session.getMapper(ExecutionBatchMapper.class);
            mapper.insert(batch);
            session.commit();
            return batch.getBatchId();
        } catch (Exception e) {
            System.err.println("插入执行批次失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 插入执行详情
     */
    public void insertDetail(ExecutionDetail detail) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ExecutionDetailMapper mapper = session.getMapper(ExecutionDetailMapper.class);
            mapper.insert(detail);
            session.commit();
        } catch (Exception e) {
            System.err.println("插入执行详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有执行批次（按时间倒序）
     */
    public List<ExecutionBatch> getAllBatches() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ExecutionBatchMapper mapper = session.getMapper(ExecutionBatchMapper.class);
            return mapper.selectAll();
        } catch (Exception e) {
            System.err.println("查询执行批次失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取指定批次的所有执行详情
     */
    public List<ExecutionDetail> getDetailsByBatchId(Long batchId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ExecutionDetailMapper mapper = session.getMapper(ExecutionDetailMapper.class);
            return mapper.selectByBatchId(batchId);
        } catch (Exception e) {
            System.err.println("查询执行详情失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取指定用例的所有执行历史
     */
    public List<ExecutionDetail> getDetailsByCaseId(String caseId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ExecutionDetailMapper mapper = session.getMapper(ExecutionDetailMapper.class);
            return mapper.selectByCaseId(caseId);
        } catch (Exception e) {
            System.err.println("查询用例执行历史失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 删除指定批次及其详情
     */
    public void deleteBatch(Long batchId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ExecutionDetailMapper detailMapper = session.getMapper(ExecutionDetailMapper.class);
            ExecutionBatchMapper batchMapper = session.getMapper(ExecutionBatchMapper.class);

            detailMapper.deleteByBatchId(batchId);
            batchMapper.deleteById(batchId);

            session.commit();
        } catch (Exception e) {
            System.err.println("删除执行批次失败: " + e.getMessage());
        }
    }

    /**
     * 清理过期数据（保留最近 N 天）
     */
    public void cleanOldData(int daysToKeep) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ExecutionDetailMapper mapper = session.getMapper(ExecutionDetailMapper.class);
            mapper.deleteOldDetails(daysToKeep);

            // 删除没有详情的批次
            session.getConnection().createStatement().execute(
                "DELETE FROM execution_batch WHERE batch_id NOT IN (SELECT DISTINCT batch_id FROM execution_detail)");

            session.commit();
        } catch (Exception e) {
            System.err.println("清理过期数据失败: " + e.getMessage());
        }
    }

    /**
     * 清除所有执行历史
     */
    public void clearAllHistory() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ExecutionDetailMapper detailMapper = session.getMapper(ExecutionDetailMapper.class);
            ExecutionBatchMapper batchMapper = session.getMapper(ExecutionBatchMapper.class);

            detailMapper.deleteAllDetails();
            batchMapper.deleteAll();

            session.commit();
        } catch (Exception e) {
            System.err.println("清除所有执行历史失败: " + e.getMessage());
        }
    }
}

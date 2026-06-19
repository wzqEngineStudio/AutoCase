package com.autocase.dao;

import com.autocase.dao.mapper.DefectMapper;
import com.autocase.entity.Defect;
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
 * 缺陷 DAO - 使用 MyBatis 管理 H2 数据库
 */
public class DefectDao {

    private final SqlSessionFactory sqlSessionFactory;

    public DefectDao() {
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
        String createDefectSql = """
            CREATE TABLE IF NOT EXISTS defect (
                defect_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                defect_title VARCHAR(200),
                case_id VARCHAR(50),
                case_name VARCHAR(100),
                source VARCHAR(20),
                severity VARCHAR(20),
                status VARCHAR(20),
                description VARCHAR(2000),
                steps VARCHAR(2000),
                expected_result VARCHAR(2000),
                actual_result VARCHAR(2000),
                attachment_path VARCHAR(500),
                reporter VARCHAR(50),
                assignee VARCHAR(50),
                created_time TIMESTAMP,
                updated_time TIMESTAMP,
                remark VARCHAR(1000)
            )
            """;

        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createDefectSql);
        } catch (SQLException e) {
            System.err.println("初始化缺陷数据库失败: " + e.getMessage());
        }
    }

    /**
     * 插入缺陷
     */
    public Long insertDefect(Defect defect) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            DefectMapper mapper = session.getMapper(DefectMapper.class);
            mapper.insert(defect);
            session.commit();
            return defect.getDefectId();
        } catch (Exception e) {
            System.err.println("插入缺陷失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 更新缺陷状态
     */
    public void updateDefect(Defect defect) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            DefectMapper mapper = session.getMapper(DefectMapper.class);
            mapper.update(defect);
            session.commit();
        } catch (Exception e) {
            System.err.println("更新缺陷失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有缺陷
     */
    public List<Defect> getAllDefects() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            DefectMapper mapper = session.getMapper(DefectMapper.class);
            return mapper.selectAll();
        } catch (Exception e) {
            System.err.println("查询缺陷失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 根据用例ID获取缺陷
     */
    public List<Defect> getDefectsByCaseId(String caseId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            DefectMapper mapper = session.getMapper(DefectMapper.class);
            return mapper.selectByCaseId(caseId);
        } catch (Exception e) {
            System.err.println("查询用例缺陷失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 根据来源获取缺陷（MANUAL / AUTO）
     */
    public List<Defect> getDefectsBySource(String source) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            DefectMapper mapper = session.getMapper(DefectMapper.class);
            return mapper.selectBySource(source);
        } catch (Exception e) {
            System.err.println("查询来源缺陷失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 删除缺陷
     */
    public void deleteDefect(Long defectId) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            DefectMapper mapper = session.getMapper(DefectMapper.class);
            mapper.deleteById(defectId);
            session.commit();
        } catch (Exception e) {
            System.err.println("删除缺陷失败: " + e.getMessage());
        }
    }

    /**
     * 清除所有缺陷
     */
    public void clearAllDefects() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            DefectMapper mapper = session.getMapper(DefectMapper.class);
            mapper.deleteAll();
            session.commit();
        } catch (Exception e) {
            System.err.println("清除所有缺陷失败: " + e.getMessage());
        }
    }
}

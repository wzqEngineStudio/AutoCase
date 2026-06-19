package com.autocase.dao.mapper;

import com.autocase.entity.ExecutionBatch;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 执行批次 Mapper 接口
 */
public interface ExecutionBatchMapper {

    @Insert("INSERT INTO execution_batch (trigger_type, trigger_user, total_cases, passed_count, failed_count, blocked_count, start_time, end_time, git_commit) " +
            "VALUES (#{triggerType}, #{triggerUser}, #{totalCases}, #{passedCount}, #{failedCount}, #{blockedCount}, #{startTime}, #{endTime}, #{gitCommit})")
    @Options(useGeneratedKeys = true, keyProperty = "batchId")
    int insert(ExecutionBatch batch);

    @Select("SELECT * FROM execution_batch ORDER BY start_time DESC")
    List<ExecutionBatch> selectAll();

    @Select("SELECT * FROM execution_batch WHERE batch_id = #{batchId}")
    ExecutionBatch selectById(Long batchId);

    @Delete("DELETE FROM execution_batch WHERE batch_id = #{batchId}")
    int deleteById(Long batchId);

    @Delete("DELETE FROM execution_batch")
    int deleteAll();
}

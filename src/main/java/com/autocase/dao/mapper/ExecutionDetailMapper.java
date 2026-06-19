package com.autocase.dao.mapper;

import com.autocase.entity.ExecutionDetail;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 执行详情 Mapper 接口
 */
public interface ExecutionDetailMapper {

    @Insert("INSERT INTO execution_detail (batch_id, case_id, status, duration_ms, failure_reason, output_log, stack_trace, execution_time, expected_result, actual_result) " +
            "VALUES (#{batchId}, #{caseId}, #{status}, #{durationMs}, #{failureReason}, #{outputLog}, #{stackTrace}, #{executionTime}, #{expectedResult}, #{actualResult})")
    @Options(useGeneratedKeys = true, keyProperty = "detailId")
    int insert(ExecutionDetail detail);

    @Select("SELECT * FROM execution_detail WHERE batch_id = #{batchId} ORDER BY execution_time")
    List<ExecutionDetail> selectByBatchId(Long batchId);

    @Select("SELECT * FROM execution_detail WHERE case_id = #{caseId} ORDER BY execution_time DESC")
    List<ExecutionDetail> selectByCaseId(String caseId);

    @Delete("DELETE FROM execution_detail WHERE batch_id = #{batchId}")
    int deleteByBatchId(Long batchId);

    @Delete("DELETE FROM execution_detail WHERE batch_id IN (" +
            "SELECT batch_id FROM execution_batch WHERE start_time < TIMESTAMPADD(DAY, -#{days}, CURRENT_TIMESTAMP()))")
    int deleteOldDetails(int days);

    @Delete("DELETE FROM execution_detail")
    int deleteAllDetails();
}

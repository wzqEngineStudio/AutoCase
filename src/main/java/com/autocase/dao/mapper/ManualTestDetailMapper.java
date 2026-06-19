package com.autocase.dao.mapper;

import com.autocase.entity.ManualTestDetail;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 手工测试详情 Mapper 接口
 */
public interface ManualTestDetailMapper {

    @Insert("INSERT INTO manual_test_detail (batch_id, case_id, case_name, status, expected_result, actual_result, failure_reason, attachment_path, remark, linked_auto_case_id, priority, severity, execution_time) " +
            "VALUES (#{batchId}, #{caseId}, #{caseName}, #{status}, #{expectedResult}, #{actualResult}, #{failureReason}, #{attachmentPath}, #{remark}, #{linkedAutoCaseId}, #{priority}, #{severity}, #{executionTime})")
    @Options(useGeneratedKeys = true, keyProperty = "detailId")
    int insert(ManualTestDetail detail);

    @Update("UPDATE manual_test_detail SET status=#{status}, expected_result=#{expectedResult}, actual_result=#{actualResult}, failure_reason=#{failureReason}, attachment_path=#{attachmentPath}, remark=#{remark}, linked_auto_case_id=#{linkedAutoCaseId}, priority=#{priority}, severity=#{severity} WHERE detail_id=#{detailId}")
    int update(ManualTestDetail detail);

    @Select("SELECT * FROM manual_test_detail WHERE batch_id = #{batchId} ORDER BY execution_time")
    List<ManualTestDetail> selectByBatchId(Long batchId);

    @Select("SELECT * FROM manual_test_detail WHERE case_id = #{caseId} ORDER BY execution_time DESC")
    List<ManualTestDetail> selectByCaseId(String caseId);

    @Delete("DELETE FROM manual_test_detail WHERE batch_id = #{batchId}")
    int deleteByBatchId(Long batchId);

    @Delete("DELETE FROM manual_test_detail")
    int deleteAllDetails();
}

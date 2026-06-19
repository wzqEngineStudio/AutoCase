package com.autocase.dao.mapper;

import com.autocase.entity.ManualTestBatch;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 手工测试批次 Mapper 接口
 */
public interface ManualTestBatchMapper {

    @Insert("INSERT INTO manual_test_batch (group_id, task_name, version, module_name, tester, total_cases, passed_count, failed_count, blocked_count, pending_count, start_time, end_time, remark) " +
            "VALUES (#{groupId}, #{taskName}, #{version}, #{moduleName}, #{tester}, #{totalCases}, #{passedCount}, #{failedCount}, #{blockedCount}, #{pendingCount}, #{startTime}, #{endTime}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "batchId")
    int insert(ManualTestBatch batch);

    @Select("SELECT * FROM manual_test_batch ORDER BY start_time DESC")
    List<ManualTestBatch> selectAll();

    @Select("SELECT * FROM manual_test_batch WHERE batch_id = #{batchId}")
    ManualTestBatch selectById(Long batchId);

    @Select("SELECT * FROM manual_test_batch WHERE group_id = #{groupId} ORDER BY start_time DESC")
    List<ManualTestBatch> selectByGroupId(Long groupId);

    @Delete("DELETE FROM manual_test_batch WHERE batch_id = #{batchId}")
    int deleteById(Long batchId);

    @Delete("DELETE FROM manual_test_batch WHERE group_id = #{groupId}")
    int deleteByGroupId(Long groupId);

    @Delete("DELETE FROM manual_test_batch")
    int deleteAll();

    @Update("UPDATE manual_test_batch SET group_id=#{groupId}, task_name=#{taskName}, version=#{version}, module_name=#{moduleName}, tester=#{tester}, total_cases=#{totalCases}, passed_count=#{passedCount}, failed_count=#{failedCount}, blocked_count=#{blockedCount}, pending_count=#{pendingCount}, start_time=#{startTime}, end_time=#{endTime}, remark=#{remark} WHERE batch_id=#{batchId}")
    int update(ManualTestBatch batch);
}

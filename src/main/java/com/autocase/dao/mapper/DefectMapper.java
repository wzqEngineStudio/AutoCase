package com.autocase.dao.mapper;

import com.autocase.entity.Defect;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 缺陷 Mapper 接口
 */
public interface DefectMapper {

    @Insert("INSERT INTO defect (defect_title, case_id, case_name, source, severity, status, description, steps, expected_result, actual_result, attachment_path, reporter, assignee, created_time, updated_time, remark) " +
            "VALUES (#{defectTitle}, #{caseId}, #{caseName}, #{source}, #{severity}, #{status}, #{description}, #{steps}, #{expectedResult}, #{actualResult}, #{attachmentPath}, #{reporter}, #{assignee}, #{createdTime}, #{updatedTime}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "defectId")
    int insert(Defect defect);

    @Update("UPDATE defect SET status=#{status}, updated_time=#{updatedTime}, remark=#{remark} WHERE defect_id=#{defectId}")
    int update(Defect defect);

    @Select("SELECT * FROM defect ORDER BY created_time DESC")
    List<Defect> selectAll();

    @Select("SELECT * FROM defect WHERE defect_id = #{defectId}")
    Defect selectById(Long defectId);

    @Select("SELECT * FROM defect WHERE case_id = #{caseId} ORDER BY created_time DESC")
    List<Defect> selectByCaseId(String caseId);

    @Select("SELECT * FROM defect WHERE source = #{source} ORDER BY created_time DESC")
    List<Defect> selectBySource(String source);

    @Delete("DELETE FROM defect WHERE defect_id = #{defectId}")
    int deleteById(Long defectId);

    @Delete("DELETE FROM defect")
    int deleteAll();
}

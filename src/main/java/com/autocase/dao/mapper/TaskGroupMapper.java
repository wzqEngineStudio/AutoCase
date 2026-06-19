package com.autocase.dao.mapper;

import com.autocase.entity.TaskGroup;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 任务组 Mapper 接口
 */
public interface TaskGroupMapper {

    @Insert("INSERT INTO task_group (group_name, description, create_time, remark) " +
            "VALUES (#{groupName}, #{description}, #{createTime}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "groupId")
    int insert(TaskGroup group);

    @Select("SELECT * FROM task_group ORDER BY create_time DESC")
    List<TaskGroup> selectAll();

    @Select("SELECT * FROM task_group WHERE group_id = #{groupId}")
    TaskGroup selectById(Long groupId);

    @Select("SELECT * FROM task_group WHERE group_name = #{groupName}")
    TaskGroup selectByName(String groupName);

    @Delete("DELETE FROM task_group WHERE group_id = #{groupId}")
    int deleteById(Long groupId);

    @Delete("DELETE FROM task_group")
    int deleteAll();

    @Update("UPDATE task_group SET group_name=#{groupName}, description=#{description}, remark=#{remark} WHERE group_id=#{groupId}")
    int update(TaskGroup group);
}

package com.project.demo.mapper;

import com.project.demo.entity.ForumSection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ForumSectionMapper {

    // 查询所有启用的板块
    List<ForumSection> selectAllEnabled();

    // 根据ID查询
    ForumSection selectById(@Param("id") Long id);

    // 原子增减帖子数量
    void incPostCount(@Param("id") Long id, @Param("delta") Integer delta);
}

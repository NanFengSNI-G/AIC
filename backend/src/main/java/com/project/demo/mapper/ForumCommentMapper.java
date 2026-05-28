package com.project.demo.mapper;

import com.project.demo.entity.ForumComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ForumCommentMapper {

    // 分页查询一级评论
    List<ForumComment> selectRootComments(@Param("postId") Long postId,
                                          @Param("offset") Integer offset,
                                          @Param("size") Integer size);

    // 统计一级评论数
    Long countRootComments(@Param("postId") Long postId);

    // 批量查询子评论（多个父评论ID）
    List<ForumComment> selectRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    // 根据ID查询
    ForumComment selectById(@Param("id") Long id);

    // 插入评论
    void insert(ForumComment comment);

    // 原子增减点赞数
    void incLikeCount(@Param("id") Long id, @Param("delta") Integer delta);

    // 更新状态
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);
}

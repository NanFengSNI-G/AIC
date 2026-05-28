package com.project.demo.mapper;

import com.project.demo.entity.ForumPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ForumPostMapper {

    // 分页查询帖子列表（关联用户详情和板块）
    List<ForumPost> selectPostList(@Param("sectionId") Long sectionId,
                                   @Param("userId") Long userId,
                                   @Param("sortBy") String sortBy,
                                   @Param("offset") Integer offset,
                                   @Param("size") Integer size);

    // 统计帖子数量
    Long countPostList(@Param("sectionId") Long sectionId);

    // 根据ID查询
    ForumPost selectById(@Param("id") Long id);

    // 插入帖子
    void insert(ForumPost post);

    // 更新帖子
    void update(ForumPost post);

    // 原子增减点赞数
    void incLikeCount(@Param("id") Long id, @Param("delta") Integer delta);

    // 原子增减评论数
    void incCommentCount(@Param("id") Long id, @Param("delta") Integer delta);

    // 原子增减浏览数（批量同步使用）
    void incViewCount(@Param("id") Long id, @Param("delta") Integer delta);

    // 查询用户帖子列表
    List<ForumPost> selectByUserId(@Param("userId") Long userId,
                                   @Param("offset") Integer offset,
                                   @Param("size") Integer size);

    // 统计用户帖子数
    Long countByUserId(@Param("userId") Long userId);

    List<ForumPost> selectAllNormal();
}

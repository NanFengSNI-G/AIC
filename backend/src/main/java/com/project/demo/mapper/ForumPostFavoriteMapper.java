package com.project.demo.mapper;

import com.project.demo.entity.ForumPost;
import com.project.demo.entity.ForumPostFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ForumPostFavoriteMapper {

    // 查询收藏记录
    ForumPostFavorite selectOne(@Param("postId") Long postId, @Param("userId") Long userId);

    // 插入收藏记录
    void insert(ForumPostFavorite favorite);

    // 删除收藏记录
    void delete(@Param("postId") Long postId, @Param("userId") Long userId);

    // 查询用户收藏的帖子列表
    List<ForumPost> selectUserFavorites(@Param("userId") Long userId,
                                        @Param("offset") Integer offset,
                                        @Param("size") Integer size);

    // 统计用户收藏数
    Long countUserFavorites(@Param("userId") Long userId);

    // 从指定帖子ID列表中筛选出用户已收藏的帖子ID
    List<Long> selectFavoritedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}

package com.project.demo.mapper;

import com.project.demo.entity.UserDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户详情 Mapper
 */
@Mapper
public interface UserDetailMapper {

    /** 根据 userId 查询详情 */
    UserDetail selectByUserId(@Param("userId") Long userId);

    /** 批量查询用户详情 */
    List<UserDetail> selectByUserIds(@Param("userIds") List<Long> userIds);

    /** 插入用户详情 */
    int insert(@Param("userId") Long userId, @Param("username") String username);

    /** 更新用户详情 */
    int updateByUserId(UserDetail detail);

    /** 根据用户名查询（检查唯一性） */
    UserDetail selectByUsername(@Param("username") String username);

    /**
     * 搜索用户并返回好友关系状态
     * @return List<Map> 包含用户信息和 friendStatus 字段
     */
    List<Map<String, Object>> searchUsersWithFriendStatus(@Param("keyword") String keyword,
                                                          @Param("currentUserId") Long currentUserId,
                                                          @Param("offset") int offset,
                                                          @Param("size") int size);

    /**
     * 统计搜索结果数量（带好友关系状态）
     */
    long countSearchUsersWithFriendStatus(@Param("keyword") String keyword,
                                          @Param("currentUserId") Long currentUserId);


}
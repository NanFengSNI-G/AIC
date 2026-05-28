package com.project.demo.mapper;

import com.project.demo.entity.Friend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 好友 Mapper
 * → 在 FriendMapper.xml 中，用 <select>、<insert> 等标签定义
 * → MyBatis 会自动将接口方法和 XML 中的 SQL 关联起来
 */
@Mapper
public interface FriendMapper {

    /**
     * 分页查询用户的好友列表
     *
     * @param userId 用户ID
     * @param search 搜索关键字（可选，为空时不搜索）
     * @param offset 偏移量
     * @param size 每页条数
     * @return 好友列表
     */
    List<Friend> selectFriendsByUserId(@Param("userId") Long userId,
                                       @Param("search") String search,
                                       @Param("offset") Integer offset,
                                       @Param("size") Integer size);

    /**
     * 统计用户的好友数量
     *
     * @param userId 用户ID
     * @param search 搜索关键字（可选，为空时不搜索）
     * @return 好友数量
     */
    Long countFriendsByUserId(@Param("userId") Long userId, @Param("search") String search);

    /**
     * 分页查询发给用户的好友申请

     * 筛选条件说明：
     * → friend_id = #{userId}    因为是「发给当前用户的」申请
     * → status = 0               只看「待确认」的申请

     * 为什么不查 user_id = #{userId} 的记录？
     * → user_id = #{userId} 的记录是「我发给别人的」申请
     * → 这是别人发给我的，所以查 friend_id = #{userId}
     */
    List<Friend> selectFriendRequestsByUserId(@Param("userId") Long userId,
                                              @Param("search") String search,
                                              @Param("offset") Integer offset,
                                              @Param("size") Integer size);

    /**
     * 统计收到的申请数量
     */
    Long countFriendRequestsByUserId(@Param("userId") Long userId, @Param("search") String search);

    /**
     * 查询单向记录（用于判断是否已存在申请/好友关系）
     * @return 一条记录或 null
     */
    Friend selectOneWay(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * 一次查询双向记录（代替两次 selectOneWay）
     * @return 两个方向的记录（0~2 条）
     */
    List<Friend> selectBothWays(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 根据ID查询好友关系
     */
    Friend selectById(@Param("id") Long id);

    /**
     * 插入好友关系
     */
    void insertFriend(Friend friend);

    /**
     * 更新好友关系（通过ID，全量更新）
     */
    void updateById(Friend friend);

    /**
     * 只更新 status（拒绝 / 软删除）
     */
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新 status + update_time（同意申请）
     */
    void updateStatusAndUpdateTime(@Param("id") Long id, @Param("status") Integer status,
                                   @Param("updateTime") java.time.LocalDateTime updateTime);
}

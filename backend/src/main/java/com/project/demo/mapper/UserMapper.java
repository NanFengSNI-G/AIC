package com.project.demo.mapper;

import com.project.demo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper
 * 继承 BaseMapper 后自动获得 CRUD 方法
 * 自定义方法通过 XML 实现
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);

    /**
     * 根据邮箱查询用户
     */
    User findByEmail(String email);

    /**
     * 根据ID查询用户
     */
    User findById(Long id);

    /**
     * 插入用户角色关联
     */
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 更新密码
     */
    void updatePassword(@Param("userId") Long userId, @Param("password") String password);

    /**
     * 插入用户
     */
    void insertUser(User user);
}

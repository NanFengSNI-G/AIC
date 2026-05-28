package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页请求 DTO
 * 为什么需要分页？
 * → 如果用户有 10000 个好友，一次性返回会造成：
 1. 数据库查询慢（大量数据）
 2. 网络传输慢（大 JSON）
 3. 前端渲染慢（大量 DOM）
 * → 分页让每次只返回固定数量的数据，如 20 条
 * 前端传参示例：
 * GET /api/friend/list?page=1&size=20   ← 第1页，每页20条
 * GET /api/friend/list?page=3&size=20   ← 第3页，每页20条
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    /**
     * 当前页码
     * 默认 1，表示第一页
     * 最小值为 1
     */
    private Integer page = 1;

    /**
     * 每页条数
     * 默认 20，建议不超过 100
     * 值太大会影响性能
     */
    private Integer size = 20;

    /**
     * 搜索关键字
     * 默认为空
     */
    private String search;

    /**
     * 计算 OFFSET（偏移量）
     * 为什么需要这个方法？
     * → SQL 的 LIMIT 需要两个参数：LIMIT offset, size
     * → 第1页：OFFSET = (1-1)*20 = 0
     * → 第3页：OFFSET = (3-1)*20 = 40
     * MySQL LIMIT 的工作原理：
     * LIMIT 0, 20   → 从第1条开始，取20条
     * LIMIT 20, 20  → 从第21条开始，取20条
     * LIMIT 40, 20  → 从第41条开始，取20条
     */
    public Integer getOffset() {
        return (page - 1) * size;
    }
}

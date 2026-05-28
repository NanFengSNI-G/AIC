package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应 DTO

 * 为什么需要单独的响应类？
 * → 除了 list（当前页数据），还需要 total（总条数）
 * → 前端根据 total 可以计算：总页数 = ceil(total / size)
 * → 前端根据 total 可以显示：「共 100 条记录，第 1/5 页」

 * 响应结构：
 * {
 *   "list": [...],      ← 当前页的数据列表
 *   "total": 100,       ← 总共有多少条（不是当前页的条数）
 *   "page": 1,          ← 当前页码
 *   "size": 20          ← 每页条数
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * 当前页的数据列表
     * 泛型 T 可以是 FriendResponse 等任何类型
     */
    private List<T> list;

    /**
     * 总记录数
     * 注意：这是「总共有多少条」，不是「当前页有多少条」
     * 用于前端计算总页数和分页控件
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页条数
     */
    private Integer size;

    /**
     * 工厂方法，方便创建
     * 为什么用静态工厂方法而不是构造函数？
     * → 语义更清晰：PageResponse.of(list, total, page, size)
     * → 代码可读性更好
     */
    public static <T> PageResponse<T> of(List<T> list, Long total, int page, int size) {
        return new PageResponse<>(list, total, page, size);
    }
}

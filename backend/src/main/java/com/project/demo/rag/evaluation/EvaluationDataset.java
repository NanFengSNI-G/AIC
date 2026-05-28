package com.project.demo.rag.evaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估数据集 —— 包含测试用例和期望的检索结果。
 */
public class EvaluationDataset {

    private final List<EvalCase> cases;

    public EvaluationDataset() {
        this.cases = new ArrayList<>();
        buildBuiltInCases();
    }

    public List<EvalCase> getCases() {
        return cases;
    }

    public void addCase(EvalCase c) {
        cases.add(c);
    }

    /**
     * 构建内置评估用例，覆盖四个知识领域。
     */
    private void buildBuiltInCases() {
        // ── Java 基础 ──
        cases.add(new EvalCase(
                "HashMap 的底层数据结构是什么？",
                "数组 + 链表 + 红黑树",
                List.of("HashMap", "数组", "链表", "红黑树", "底层数据结构")
        ));
        cases.add(new EvalCase(
                "synchronized 和 ReentrantLock 有什么区别？",
                "synchronized 是 JVM 层面的隐式锁，ReentrantLock 是 API 层面的显式锁",
                List.of("synchronized", "ReentrantLock", "锁", "区别", "并发")
        ));
        cases.add(new EvalCase(
                "什么是 Java 的动态代理？",
                "在运行时动态生成代理类，JDK 动态代理和 CGLIB 代理",
                List.of("动态代理", "JDK", "CGLIB", "Proxy")
        ));
        cases.add(new EvalCase(
                "volatile 关键字的作用是什么？",
                "保证可见性和禁止指令重排序，不保证原子性",
                List.of("volatile", "可见性", "指令重排序", "JMM")
        ));

        // ── Java 集合 ──
        cases.add(new EvalCase(
                "ArrayList 和 LinkedList 的区别？",
                "ArrayList 基于数组，LinkedList 基于双向链表",
                List.of("ArrayList", "LinkedList", "数组", "链表", "区别")
        ));
        cases.add(new EvalCase(
                "ConcurrentHashMap 是如何保证线程安全的？",
                "JDK7 分段锁，JDK8 CAS + synchronized 锁链表头节点",
                List.of("ConcurrentHashMap", "线程安全", "CAS", "synchronized", "分段锁")
        ));
        cases.add(new EvalCase(
                "HashSet 的底层实现是什么？",
                "基于 HashMap，元素作为 key，value 是 PRESENT 常量",
                List.of("HashSet", "HashMap", "底层实现")
        ));
        cases.add(new EvalCase(
                "TreeMap 的排序原理？",
                "基于红黑树，使用自然排序或 Comparator 定制排序",
                List.of("TreeMap", "红黑树", "排序", "Comparator")
        ));

        // ── MySQL ──
        cases.add(new EvalCase(
                "MySQL 的索引底层数据结构是什么？",
                "B+ 树，叶子节点存储数据且形成有序链表",
                List.of("MySQL", "索引", "B+树", "叶子节点", "数据结构")
        ));
        cases.add(new EvalCase(
                "什么是 MySQL 的事务隔离级别？",
                "READ-UNCOMMITTED, READ-COMMITTED, REPEATABLE-READ, SERIALIZABLE",
                List.of("事务", "隔离级别", "READ-UNCOMMITTED", "READ-COMMITTED", "REPEATABLE-READ", "SERIALIZABLE")
        ));
        cases.add(new EvalCase(
                "MySQL 的 MVCC 原理是什么？",
                "多版本并发控制，通过 undo log 和 ReadView 实现",
                List.of("MVCC", "undo log", "ReadView", "多版本并发控制")
        ));
        cases.add(new EvalCase(
                "什么是慢查询？如何优化？",
                "执行时间超过阈值的 SQL，通过 EXPLAIN 分析执行计划",
                List.of("慢查询", "EXPLAIN", "执行计划", "优化")
        ));
        cases.add(new EvalCase(
                "MySQL 的主从复制原理？",
                "binlog + I/O线程 + SQL线程",
                List.of("主从复制", "binlog", "I/O线程", "SQL线程")
        ));

        // ── Redis ──
        cases.add(new EvalCase(
                "Redis 的数据类型有哪些？",
                "String, Hash, List, Set, ZSet",
                List.of("Redis", "数据类型", "String", "Hash", "List", "Set", "ZSet")
        ));
        cases.add(new EvalCase(
                "什么是缓存穿透？如何解决？",
                "查询不存在的数据导致请求直达数据库，解决方案有布隆过滤器和缓存空值",
                List.of("缓存穿透", "布隆过滤器", "缓存空值")
        ));
        cases.add(new EvalCase(
                "Redis 的持久化机制有哪些？",
                "RDB 快照和 AOF 日志",
                List.of("RDB", "AOF", "持久化", "快照", "日志")
        ));
        cases.add(new EvalCase(
                "Redis 的过期删除策略是什么？",
                "惰性删除 + 定期删除",
                List.of("过期删除", "惰性删除", "定期删除")
        ));
        cases.add(new EvalCase(
                "什么是缓存雪崩？",
                "大量缓存同时过期导致请求直接打到数据库",
                List.of("缓存雪崩", "同时过期", "数据库")
        ));
        cases.add(new EvalCase(
                "Redis 集群模式有哪些？",
                "主从复制、哨兵模式、Cluster 集群",
                List.of("集群", "主从复制", "哨兵", "Cluster")
        ));
    }

    /**
     * 单条评估用例。
     */
    public record EvalCase(String query, String expectedAnswer, List<String> mustRetrieveKeywords) {}
}

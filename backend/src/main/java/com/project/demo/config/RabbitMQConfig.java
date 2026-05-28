package com.project.demo.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置: 声明队列、交换机、绑定关系
 */
@Configuration
public class RabbitMQConfig {

    // ========== 交换机名称 ==========
    /** 聊天消息交换机 */
    public static final String CHAT_EXCHANGE = "chat.exchange";

    /** 面试问答广播交换机 */
    public static final String INTERVIEW_EXCHANGE = "interview.exchange";

    // ========== 队列名称 ==========
    /** 聊天消息队列 */
    public static final String CHAT_QUEUE = "chat.offline.queue";
    public static final String CHAT_ROUTING_KEY = "chat.offline";

    /** 面试问答队列 - 存储问答记录 */
    public static final String INTERVIEW_QA_QUEUE = "interview.qa.queue";

    // ========== 死信配置 ==========
    public static final String CHAT_DLX = "chat.dlx";
    public static final String CHAT_DLQ = "chat.dlq";
    public static final String CHAT_DLQ_ROUTING_KEY = "chat.dlq";

    // ========== 聊天相关==========
    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(CHAT_EXCHANGE);
    }

    @Bean
    public Queue chatQueue() {
        return QueueBuilder.durable(CHAT_QUEUE)
                .withArgument("x-dead-letter-exchange", CHAT_DLX)
                .withArgument("x-dead-letter-routing-key", CHAT_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding chatBinding(Queue chatQueue, DirectExchange chatExchange) {
        return BindingBuilder.bind(chatQueue).to(chatExchange).with(CHAT_ROUTING_KEY);
    }

    @Bean
    public DirectExchange chatDlx() {
        return new DirectExchange(CHAT_DLX);
    }

    @Bean
    public Queue chatDlq() {
        return QueueBuilder.durable(CHAT_DLQ).build();
    }

    @Bean
    public Binding chatDlqBinding(Queue chatDlq, DirectExchange chatDlx) {
        return BindingBuilder.bind(chatDlq).to(chatDlx).with(CHAT_DLQ_ROUTING_KEY);
    }

    // ========== 面试问答 ==========
    @Bean
    public FanoutExchange interviewExchange() {
        return new FanoutExchange(INTERVIEW_EXCHANGE);
    }

    @Bean
    public Queue interviewQAQueue() {
        return QueueBuilder.durable(INTERVIEW_QA_QUEUE).build();
    }

    @Bean
    public Binding interviewQABinding(Queue interviewQAQueue, FanoutExchange interviewExchange) {
        return BindingBuilder.bind(interviewQAQueue).to(interviewExchange);
    }

    // ========== 论坛帖子 ES 同步 ==========
    /** 论坛帖子同步交换机 */
    public static final String FORUM_SYNC_EXCHANGE = "forum.post.exchange";

    /** 论坛帖子同步队列 */
    public static final String FORUM_SYNC_QUEUE = "forum.post.sync.queue";

    /** 论坛帖子同步路由键 */
    public static final String FORUM_SYNC_ROUTING_KEY = "forum.post.sync";

    @Bean
    public DirectExchange forumSyncExchange() {
        return new DirectExchange(FORUM_SYNC_EXCHANGE);
    }

    @Bean
    public Queue forumSyncQueue() {
        return QueueBuilder.durable(FORUM_SYNC_QUEUE).build();
    }

    @Bean
    public Binding forumSyncBinding(Queue forumSyncQueue, DirectExchange forumSyncExchange) {
        return BindingBuilder.bind(forumSyncQueue).to(forumSyncExchange).with(FORUM_SYNC_ROUTING_KEY);
    }
}
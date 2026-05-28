package com.project.demo.consumer;

import com.alibaba.fastjson2.JSON;
import com.project.demo.config.RabbitMQConfig;
import com.project.demo.dto.InterviewQAMessage;
import com.project.demo.entity.ChatMessage;
import com.project.demo.mapper.ChatMapper;
import com.project.demo.service.ChatService;
import com.project.demo.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Consumer {

    private final ChatMapper chatMapper;
    private final ChatService chatService;
    private final InterviewService interviewService;

    private final ExecutorService evaluationExecutor = Executors.newFixedThreadPool(4);

    @RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE)
    public void consumeMessage(String messageJson) {
        if (messageJson == null) {
            return;
        }
        ChatMessage message = JSON.parseObject(messageJson, ChatMessage.class);

        boolean inserted = false;
        try {
            chatMapper.insert(message);
            inserted = true;
        } catch (DuplicateKeyException e) {
            log.warn("重复消息已跳过: messageId={}", message.getMessageId());
        }

        chatService.updateRedisConversation(
                inserted,
                message.getFromUserId(),
                message.getToUserId(),
                message.getContent(),
                toEpochSecond(message.getCreateTime())
        );
        log.info("消息入库: from:{} to:{}", message.getFromUserId(), message.getToUserId());
    }

    private static long toEpochSecond(java.time.LocalDateTime time) {
        if (time == null) return System.currentTimeMillis() / 1000;
        return time.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
    }

    @RabbitListener(queues = RabbitMQConfig.INTERVIEW_QA_QUEUE)
    public void consumeInterviewQA(String messageJson) {
        if (messageJson == null) {
            return;
        }
        try {
            InterviewQAMessage message = JSON.parseObject(messageJson, InterviewQAMessage.class);
            evaluationExecutor.submit(() -> {
                try {
                    interviewService.insertQARecord(message);
                    log.info("QA记录入库成功");
                } catch (Exception e) {
                    log.info("QA记录入库失败: {}" , e.getMessage());
                }
            });
        } catch (Exception e) {
            log.info("QA消息解析失败: {}" , e.getMessage());
        }
    }
}

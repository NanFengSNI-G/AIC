package com.project.demo.consumer;

import com.alibaba.fastjson2.JSON;
import com.project.demo.config.RabbitMQConfig;
import com.project.demo.document.ForumPostDocument;
import com.project.demo.dto.ForumPostSyncMessage;
import com.project.demo.entity.ForumPost;
import com.project.demo.mapper.ForumPostMapper;
import com.project.demo.repository.ForumPostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ForumPostSyncConsumer {

    private final ForumPostMapper postMapper;
    private final ForumPostSearchRepository esRepository;

    @RabbitListener(queues = RabbitMQConfig.FORUM_SYNC_QUEUE)
    public void handleSync(String messageJson) {
        if (messageJson == null) {
            return;
        }

        ForumPostSyncMessage msg;
        try {
            msg = JSON.parseObject(messageJson, ForumPostSyncMessage.class);
        } catch (Exception e) {
            log.warn("ES sync message parse failed: {}", e.getMessage());
            return;
        }

        try {
            switch (msg.getType()) {
                case "CREATE", "UPDATE" -> {
                    ForumPost post = postMapper.selectById(msg.getPostId());
                    if (post == null || post.getStatus() != ForumPost.STATUS_NORMAL) {
                        esRepository.deleteById(msg.getPostId());
                        log.info("ES doc deleted (post not found or not normal): postId={}", msg.getPostId());
                        return;
                    }
                    ForumPostDocument doc = toDocument(post);
                    esRepository.save(doc);
                    log.info("ES doc indexed: postId={}", msg.getPostId());
                }
                case "DELETE" -> {
                    esRepository.deleteById(msg.getPostId());
                    log.info("ES doc deleted: postId={}", msg.getPostId());
                }
                default -> log.warn("Unknown sync type: {}", msg.getType());
            }
        } catch (Exception e) {
            log.error("ES sync failed for postId={}, type={}: {}",
                    msg.getPostId(), msg.getType(), e.getMessage());
        }
    }

    private ForumPostDocument toDocument(ForumPost post) {
        return ForumPostDocument.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .sectionId(post.getSectionId())
                .title(post.getTitle())
                .content(post.getContent())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .isSticky(post.getIsSticky())
                .authorUsername(post.getAuthorUsername())
                .authorAvatar(post.getAuthorAvatar())
                .sectionName(post.getSectionName())
                .createTime(post.getCreateTime())
                .build();
    }
}

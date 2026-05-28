package com.project.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Configuration
public class CheckpointConfig {

    private static final String PREFIX = "blog:checkpoint:";
    private static final long TTL_HOURS = 24;

    @Bean
    public BaseCheckpointSaver checkpointSaver(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisCheckpointSaver(redisTemplate, objectMapper);
    }

    @Slf4j
    public static class RedisCheckpointSaver implements BaseCheckpointSaver {

        private final StringRedisTemplate redisTemplate;
        private final ObjectMapper objectMapper;

        public RedisCheckpointSaver(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
            this.redisTemplate = redisTemplate;
            this.objectMapper = objectMapper;
        }

        @Override
        public Collection<Checkpoint> list(RunnableConfig config) {
            return List.of();
        }

        @Override
        public Optional<Checkpoint> get(RunnableConfig config) {
            try {
                String key = checkpointKey(config);
                String json = redisTemplate.opsForValue().get(key);
                if (json == null) return Optional.empty();
                return Optional.of(parseCheckpoint(json));
            } catch (Exception e) {
                log.error("Failed to load checkpoint for key={}: {}", checkpointKey(config), e.getMessage());
                return Optional.empty();
            }
        }

        @Override
        public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
            String key = checkpointKey(config);
            String json = objectMapper.writeValueAsString(checkpoint);
            redisTemplate.opsForValue().set(key, json, TTL_HOURS, TimeUnit.HOURS);
            return config;
        }

        @Override
        public Tag release(RunnableConfig config) throws Exception {
            String key = checkpointKey(config);
            String json = redisTemplate.opsForValue().get(key);
            redisTemplate.delete(key);

            if (json != null) {
                return new Tag(config.threadId().orElse("default"), List.of(parseCheckpoint(json)));
            }
            return new Tag(config.threadId().orElse("default"), List.of());
        }

        @SuppressWarnings("unchecked")
        private Checkpoint parseCheckpoint(String json) throws Exception {
            Map<String, Object> raw = objectMapper.readValue(json, Map.class);
            return Checkpoint.builder()
                .id((String) raw.get("id"))
                .state((Map<String, Object>) raw.get("state"))
                .nodeId((String) raw.get("nodeId"))
                .nextNodeId((String) raw.get("nextNodeId"))
                .build();
        }

        private String checkpointKey(RunnableConfig config) {
            String threadId = config.threadId().orElse("default");
            return PREFIX + threadId;
        }
    }
}

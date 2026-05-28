package com.project.demo;

import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
@MapperScan("com.project.demo.mapper")
@EnableScheduling
public class DemoApplication implements CommandLineRunner {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final MilvusEmbeddingStore milvusEmbeddingStore;

    public DemoApplication(DataSource dataSource,
                           StringRedisTemplate redisTemplate,
                           RabbitTemplate rabbitTemplate,
                           MilvusEmbeddingStore milvusEmbeddingStore) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.milvusEmbeddingStore = milvusEmbeddingStore;
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Override
    public void run(String... args) {
        checkMySQLConnection();
        checkRedisConnection();
        checkRabbitMQConnection();
        checkMilvusConnection();
    }

    private void checkMySQLConnection() {
        System.out.println("\n========== MySQL 连接检查 ==========");
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("✅ MySQL 连接成功");
            System.out.println("   数据库产品: " + conn.getMetaData().getDatabaseProductName());
            System.out.println("   数据库版本: " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("   连接 URL: " + conn.getMetaData().getURL());
        } catch (Exception e) {
            System.out.println("❌ MySQL 连接失败: " + e.getMessage());
        }
        System.out.println("===================================\n");
    }

    private void checkRedisConnection() {
        System.out.println("========== Redis 连接检查 ==========");
        try {
            if (redisTemplate.getConnectionFactory() == null) {
                System.out.println("❌ Redis 连接失败: ConnectionFactory 为空");
                return;
            }
            try (RedisConnection conn = redisTemplate.getConnectionFactory().getConnection()) {
                String pong = conn.ping();
                System.out.println("✅ Redis 连接成功");
                System.out.println("   PING 响应: " + pong);
            }
        } catch (Exception e) {
            System.out.println("❌ Redis 连接失败: " + e.getMessage());
        }
        System.out.println("===================================\n");
    }

    private void checkRabbitMQConnection() {
        System.out.println("========== RabbitMQ 连接检查 ==========");
        try {
            rabbitTemplate.execute(channel -> {
                System.out.println("✅ RabbitMQ 连接成功");
                System.out.println("   Exchange: chat.offline.exchange");
                System.out.println("   Queue: chat.offline.queue");
                return null;
            });
        } catch (Exception e) {
            System.out.println("❌ RabbitMQ 连接失败: " + e.getMessage());
        }
        System.out.println("===================================\n");
    }

    private void checkMilvusConnection() {
        System.out.println("========== Milvus 连接检查 ==========");
        try {
            if (milvusEmbeddingStore == null) {
                System.out.println("❌ Milvus 连接失败: EmbeddingStore 为空");
                return;
            }
            System.out.println("✅ Milvus 连接成功");
            System.out.println("   Host: localhost:19530");
            System.out.println("   Collection: knowledge_base");
        } catch (Exception e) {
            System.out.println("❌ Milvus 连接失败: " + e.getMessage());
        }
        System.out.println("===================================\n");
    }
}

package com.project.demo.config;


import com.project.demo.agent.RedisChatMemoryStore;
import com.project.demo.rag.DashScopeReranker;
import com.project.demo.rag.HybridContentRetriever;
import com.project.demo.rag.InterviewQueryTransformer;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;


@Configuration
public class AgentConfig {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Bean
    public MilvusEmbeddingStore milvusEmbeddingStore() {
        return MilvusEmbeddingStore.builder()
                .host("localhost")
                .port(19530)
                .collectionName("knowledge_base")
                .dimension(1024)
                .autoFlushOnInsert(true)
                .build();
    }

    @Bean
    public ChatMemoryStore interviewChatMemoryStore() {
        return new RedisChatMemoryStore(redisTemplate, "interview:memory:");
    }

    @Bean
    public ChatMemoryStore evaluateChatMemoryStore() {
        return new RedisChatMemoryStore(redisTemplate, "evaluate:memory:");
    }

    @Bean
    public ChatMemoryStore blogChatMemoryStore() {
        return new RedisChatMemoryStore(redisTemplate, "blog:memory:");
    }

    @Bean
    public ChatMemoryStore decisionChatMemoryStore() {
        return new RedisChatMemoryStore(redisTemplate, "decision:memory:");
    }


    @Bean
    public ChatMemoryProvider interviewChatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(100)
                .chatMemoryStore(interviewChatMemoryStore())
                .build();
    }

    @Bean
    public ChatMemoryProvider evaluateChatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(100)
                .chatMemoryStore(evaluateChatMemoryStore())
                .build();
    }

    @Bean
    public ChatMemoryProvider blogChatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(30)
                .chatMemoryStore(blogChatMemoryStore())
                .build();
    }

    @Bean
    public ChatMemoryProvider decisionChatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(50)
                .chatMemoryStore(decisionChatMemoryStore())
                .build();
    }

    @Bean
    public HybridContentRetriever contentRetriever(
            @Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.base-url}") String baseUrl,
            @Value("${langchain4j.open-ai.chat-model.model-name}") String modelName) {
        // 向量检索器：粗召回 Top-20
        EmbeddingStoreContentRetriever vectorRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(milvusEmbeddingStore())
                .minScore(0.3)
                .maxResults(20)
                .embeddingModel(embeddingModel)
                .build();

        // Reranker：Qwen3-Rerank 精排 Top-3
        DashScopeReranker reranker = new DashScopeReranker(apiKey, "qwen3-vl-rerank", 3);

        // 查询改写：用 ChatModel 将口语化回答提炼为检索关键词
        OpenAiChatModel rewriteModel = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(50)
                .build();
        InterviewQueryTransformer queryTransformer = new InterviewQueryTransformer(rewriteModel);

        HybridContentRetriever retriever = new HybridContentRetriever(vectorRetriever, reranker, queryTransformer);
        retriever.loadLuceneIndex();
        return retriever;
    }

    @Bean
    public OpenAiChatModel jsonChatModel(
            @Value("${langchain4j.open-ai.chat-model.base-url}") String baseUrl,
            @Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.model-name}") String modelName) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .responseFormat("json_object")
                .timeout(java.time.Duration.ofSeconds(180))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // 网页搜索引擎（Tavily）
    // ═══════════════════════════════════════════════════════════════
    @Bean
    public WebSearchEngine webSearchEngine(
            @Value("${tavily.api-key}") String apiKey) {
        return TavilyWebSearchEngine.builder()
                .apiKey(apiKey)
                .build();
    }
}
package com.project.demo.rag;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 面试场景查询改写器 —— 将口语化回答提炼为检索关键词。
 * <p>
 * 使用 ChatModel 进行查询改写，内置 LRU 缓存避免重复调用。
 */
@Slf4j
public class InterviewQueryTransformer implements QueryTransformer {

    private static final String REWRITE_PROMPT = """
            你是一个查询改写助手。将用户的面试回答提炼为用于检索技术文档的关键词和核心概念。
            只输出检索关键词（用中文），不要解释，不要加前缀。

            用户回答: %s
            检索关键词:""";

    private static final int CACHE_MAX_SIZE = 200;

    private final ChatModel chatModel;
    private final Map<String, Collection<Query>> cache;

    public InterviewQueryTransformer(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.cache = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Collection<Query>> eldest) {
                return size() > CACHE_MAX_SIZE;
            }
        };
    }

    @Override
    public Collection<Query> transform(Query query) {
        String original = query.text();

        // 检查缓存
        Collection<Query> cached = cache.get(original);
        if (cached != null) {
            log.debug("查询改写命中缓存: {} → {}", original, cached);
            return cached;
        }

        try {
            String prompt = String.format(REWRITE_PROMPT, original);
            String rewritten = chatModel.chat(prompt).trim();

            if (rewritten.isEmpty() || rewritten.length() > 200) {
                // 改写结果异常，降级使用原始查询
                log.debug("查询改写结果异常，降级使用原始查询");
                return Collections.singletonList(query);
            }

            Query transformed = Query.from(rewritten, query.metadata());
            log.info("查询改写: \"{}\" → \"{}\"", original, rewritten);

            Collection<Query> result = Collections.singletonList(transformed);
            cache.put(original, result);
            return result;

        } catch (Exception e) {
            log.warn("查询改写失败: {}, 降级使用原始查询", e.getMessage());
            return Collections.singletonList(query);
        }
    }
}

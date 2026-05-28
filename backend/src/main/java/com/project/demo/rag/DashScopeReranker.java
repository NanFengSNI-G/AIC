package com.project.demo.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.rag.content.Content;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DashScope GTE-Rerank 重排序器。
 * <p>
 * 对粗召回结果精排，输出 Top-N 相关文档注入 LLM。
 */
@Slf4j
public class DashScopeReranker {

    private static final String RERANK_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";

    private final String apiKey;
    private final String model;
    private final int topN;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DashScopeReranker(String apiKey, String model, int topN) {
        this.apiKey = apiKey;
        this.model = model;
        this.topN = topN;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 对候选文档列表进行重排序，返回 Top-N 结果。
     *
     * @param query     用户查询
     * @param documents 候选文档列表（粗召回结果）
     * @return 精排后的 Top-N 文档
     */
    public List<Content> rerank(String query, List<Content> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        if (documents.size() <= topN) {
            return new ArrayList<>(documents);
        }

        try {
            List<String> docTexts = documents.stream()
                    .map(c -> c.textSegment().text())
                    .toList();

            RerankRequest request = RerankRequest.builder()
                    .model(model)
                    .input(new RerankInput(query, docTexts))
                    .parameters(new RerankParameters(topN, true))
                    .build();

            String body = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(RERANK_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Reranker API 返回非200状态: {}, 降级使用粗召回结果", response.statusCode());
                return documents.subList(0, Math.min(topN, documents.size()));
            }

            RerankResponse rerankResp = objectMapper.readValue(response.body(), RerankResponse.class);
            List<RerankResult> results = rerankResp.getOutput().getResults();

            // 按相关性分数降序排列
            results.sort(Comparator.comparingDouble(RerankResult::getRelevanceScore).reversed());

            List<Content> reranked = new ArrayList<>();
            for (int i = 0; i < Math.min(topN, results.size()); i++) {
                int idx = results.get(i).getIndex();
                if (idx >= 0 && idx < documents.size()) {
                    reranked.add(documents.get(idx));
                }
            }

            log.info("Reranker 精排完成: {} 条候选 → {} 条最终结果", documents.size(), reranked.size());
            return reranked;

        } catch (Exception e) {
            log.warn("Reranker 调用失败: {}, 降级使用粗召回 Top-{}", e.getMessage(), topN);
            return documents.subList(0, Math.min(topN, documents.size()));
        }
    }

    // ──────────────────────────── DTO ────────────────────────────

    @Data
    @Builder
    private static class RerankRequest {
        private String model;
        private RerankInput input;
        private RerankParameters parameters;
    }

    private record RerankInput(@JsonProperty("query") String query,
                               @JsonProperty("documents") List<String> documents) {}

    @Data
    @Builder
    private static class RerankParameters {
        @JsonProperty("top_n")
        private int topN;
        @JsonProperty("return_documents")
        private boolean returnDocuments;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RerankResponse {
        private RerankOutput output;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RerankOutput {
        private List<RerankResult> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RerankResult {
        private int index;
        @JsonProperty("relevance_score")
        private double relevanceScore;
    }
}

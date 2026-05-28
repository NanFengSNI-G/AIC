package com.project.demo.rag.evaluation;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * RAG 评估指标计算 —— Context Precision, Context Recall, Faithfulness, Answer Relevancy。
 */
@Slf4j
public class EvaluationMetrics {

    private static final String FAITHFULNESS_PROMPT = """
            请判断以下陈述中的主要技术信息是否来源于给定的上下文。
            允许陈述包含一些上下文之外的通用技术常识，只要核心技术点能和上下文对应即可。
            仅回答 "是" 或 "否"。

            上下文:
            %s

            陈述:
            %s

            该陈述的主要信息是否来源于以上上下文？""";

    private static final String RELEVANCY_PROMPT = """
            请判断以下回答是否切题。仅回答 "是" 或 "否"。

            问题:
            %s

            回答:
            %s

            该回答是否切题且相关？""";

    private final ChatModel chatModel;

    public EvaluationMetrics(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 计算全部四项指标。
     */
    public EvalResult evaluate(String query, String generatedAnswer,
                                List<Content> retrievedContexts,
                                List<String> expectedKeywords) {
        double contextPrecision = computeContextPrecision(retrievedContexts, expectedKeywords);
        double contextRecall = computeContextRecall(retrievedContexts, expectedKeywords);
        double faithfulness = computeFaithfulness(query, generatedAnswer, retrievedContexts);
        double answerRelevancy = computeAnswerRelevancy(query, generatedAnswer);

        return EvalResult.builder()
                .contextPrecision(contextPrecision)
                .contextRecall(contextRecall)
                .faithfulness(faithfulness)
                .answerRelevancy(answerRelevancy)
                .build();
    }

    /**
     * Context Precision —— 检索结果中相关 chunk 的比例。
     */
    double computeContextPrecision(List<Content> contexts, List<String> expectedKeywords) {
        if (contexts == null || contexts.isEmpty()) return 0.0;

        int relevantCount = 0;
        for (Content ctx : contexts) {
            String text = ctx.textSegment().text().toLowerCase();
            for (String kw : expectedKeywords) {
                if (text.contains(kw.toLowerCase())) {
                    relevantCount++;
                    break;
                }
            }
        }
        return (double) relevantCount / contexts.size();
    }

    /**
     * Context Recall —— 检索到的相关 chunk 占应检索到的比例。
     * 简化版：判断 expectedKeywords 中有多少比例出现在检索结果中。
     */
    double computeContextRecall(List<Content> contexts, List<String> expectedKeywords) {
        if (expectedKeywords == null || expectedKeywords.isEmpty()) return 1.0;

        if (contexts == null || contexts.isEmpty()) return 0.0;

        String allRetrieved = contexts.stream()
                .map(c -> c.textSegment().text().toLowerCase())
                .reduce("", (a, b) -> a + " " + b);

        int foundCount = 0;
        for (String kw : expectedKeywords) {
            if (allRetrieved.contains(kw.toLowerCase())) {
                foundCount++;
            }
        }
        return (double) foundCount / expectedKeywords.size();
    }

    /**
     * Faithfulness —— 用 LLM 判断生成的答案是否完全基于检索到的上下文。
     */
    double computeFaithfulness(String query, String generatedAnswer, List<Content> contexts) {
        if (contexts == null || contexts.isEmpty()) return 0.0;
        if (generatedAnswer == null || generatedAnswer.isBlank()) return 0.0;

        String contextText = contexts.stream()
                .map(c -> c.textSegment().text())
                .reduce("", (a, b) -> a + "\n---\n" + b);

        String prompt = String.format(FAITHFULNESS_PROMPT, contextText, generatedAnswer);
        try {
            String response = chatModel.chat(prompt).trim();
            return response.contains("是") ? 1.0 : 0.0;
        } catch (Exception e) {
            log.warn("Faithfulness 评估失败: {}", e.getMessage());
            return 0.5; // 不确定时给中间值
        }
    }

    /**
     * Answer Relevancy —— 用 LLM 判断答案是否切题。
     */
    double computeAnswerRelevancy(String query, String generatedAnswer) {
        if (query == null || generatedAnswer == null) return 0.0;

        String prompt = String.format(RELEVANCY_PROMPT, query, generatedAnswer);
        try {
            String response = chatModel.chat(prompt).trim();
            return response.contains("是") ? 1.0 : 0.0;
        } catch (Exception e) {
            log.warn("Answer Relevancy 评估失败: {}", e.getMessage());
            return 0.5;
        }
    }

    @Data
    @Builder
    public static class EvalResult {
        private double contextPrecision;
        private double contextRecall;
        private double faithfulness;
        private double answerRelevancy;

        public double avgScore() {
            return (contextPrecision + contextRecall + faithfulness + answerRelevancy) / 4.0;
        }
    }
}

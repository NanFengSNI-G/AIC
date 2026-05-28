package com.project.demo.rag.evaluation;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 评估编排器 —— 遍历评估数据集，执行检索 + 生成 + 指标计算。
 */
@Slf4j
public class RagEvaluator {

    private final ContentRetriever contentRetriever;
    private final ChatModel chatModel;
    private final EvaluationMetrics metrics;
    private final EvaluationDataset dataset;

    public RagEvaluator(ContentRetriever contentRetriever, ChatModel chatModel) {
        this.contentRetriever = contentRetriever;
        this.chatModel = chatModel;
        this.metrics = new EvaluationMetrics(chatModel);
        this.dataset = new EvaluationDataset();
    }

    /**
     * 运行全量评估，返回评估报告。
     */
    public EvalReport runFullEvaluation() {
        List<EvaluationDataset.EvalCase> cases = dataset.getCases();
        List<EvalReport.CaseResult> caseResults = new ArrayList<>();

        double totalPrecision = 0, totalRecall = 0, totalFaithfulness = 0, totalRelevancy = 0;

        for (EvaluationDataset.EvalCase evalCase : cases) {
            try {
                // 1. 检索
                List<Content> retrieved = contentRetriever.retrieve(Query.from(evalCase.query()));

                // 2. 生成回答
                String generatedAnswer = generateAnswer(evalCase.query(), retrieved);

                // 3. 计算指标
                EvaluationMetrics.EvalResult result = metrics.evaluate(
                        evalCase.query(), generatedAnswer, retrieved, evalCase.mustRetrieveKeywords());

                caseResults.add(new EvalReport.CaseResult(
                        evalCase.query(), result.getContextPrecision(),
                        result.getContextRecall(), result.getFaithfulness(),
                        result.getAnswerRelevancy()));

                totalPrecision += result.getContextPrecision();
                totalRecall += result.getContextRecall();
                totalFaithfulness += result.getFaithfulness();
                totalRelevancy += result.getAnswerRelevancy();

            } catch (Exception e) {
                log.warn("评估用例失败: {} — {}", evalCase.query(), e.getMessage());
            }
        }

        int n = Math.max(caseResults.size(), 1);

        EvalReport report = new EvalReport();
        report.setCaseResults(caseResults);
        report.setTotalCases(cases.size());
        report.setEvaluatedCases(caseResults.size());
        report.setAvgContextPrecision(totalPrecision / n);
        report.setAvgContextRecall(totalRecall / n);
        report.setAvgFaithfulness(totalFaithfulness / n);
        report.setAvgAnswerRelevancy(totalRelevancy / n);
        report.setOverallScore((totalPrecision + totalRecall + totalFaithfulness + totalRelevancy) / (4 * n));

        log.info("评估完成: {} 条用例, 综合得分 = {}", caseResults.size(),
                String.format("%.2f", report.getOverallScore()));
        return report;
    }

    private String generateAnswer(String query, List<Content> contexts) {
        if (contexts.isEmpty()) {
            return chatModel.chat(query);
        }

        StringBuilder ctx = new StringBuilder();
        for (Content c : contexts) {
            ctx.append(c.textSegment().text()).append("\n---\n");
        }

        String prompt = String.format(
                "请基于以下参考资料回答问题。\n\n参考资料:\n%s\n\n问题: %s\n\n回答:",
                ctx.toString(), query);

        return chatModel.chat(prompt);
    }
}

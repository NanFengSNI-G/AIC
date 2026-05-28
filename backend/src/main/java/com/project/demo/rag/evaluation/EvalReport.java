package com.project.demo.rag.evaluation;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估报告 —— 包含汇总指标和逐用例详情。
 */
@Data
public class EvalReport {
    private int totalCases;
    private int evaluatedCases;
    private double avgContextPrecision;
    private double avgContextRecall;
    private double avgFaithfulness;
    private double avgAnswerRelevancy;
    private double overallScore;
    private List<CaseResult> caseResults = new ArrayList<>();

    @Data
    public static class CaseResult {
        private final String query;
        private final double contextPrecision;
        private final double contextRecall;
        private final double faithfulness;
        private final double answerRelevancy;

        public CaseResult(String query, double cp, double cr, double f, double ar) {
            this.query = query;
            this.contextPrecision = cp;
            this.contextRecall = cr;
            this.faithfulness = f;
            this.answerRelevancy = ar;
        }
    }
}

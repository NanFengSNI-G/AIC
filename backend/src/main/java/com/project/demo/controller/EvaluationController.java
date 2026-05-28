package com.project.demo.controller;

import com.project.demo.rag.evaluation.EvalReport;
import com.project.demo.rag.evaluation.RagEvaluator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 评估接口 —— 运行评估并返回四项指标得分。
 */
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    @Autowired
    private ContentRetriever contentRetriever;

    @Autowired
    @Qualifier("openAiChatModel")
    private ChatModel chatModel;

    @PostMapping("/run")
    public ResponseEntity<EvalReport> runEvaluation() {
        RagEvaluator evaluator = new RagEvaluator(contentRetriever, chatModel);
        EvalReport report = evaluator.runFullEvaluation();
        return ResponseEntity.ok(report);
    }
}

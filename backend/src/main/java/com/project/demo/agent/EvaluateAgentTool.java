package com.project.demo.agent;

import com.project.demo.entity.InterviewQA;
import com.project.demo.mapper.InterviewQAMapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 评估 Agent 工具集
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluateAgentTool {

    private final InterviewQAMapper interviewQAMapper;

    @Tool("获取指定面试记录的所有问答历史")
    public String getInterviewHistoryByRecordId(Long recordId) {
        List<InterviewQA> qaList = interviewQAMapper.selectByRecordId(recordId);
        if (qaList == null || qaList.isEmpty()) {
            return "暂无问答记录";
        }

        StringBuilder sb = new StringBuilder("面试问答历史：\n");
        for (int i = 0; i < qaList.size(); i++) {
            InterviewQA qa = qaList.get(i);
            sb.append("【第").append(i + 1).append("题】\n");
            sb.append("问题：").append(qa.getQuestion()).append("\n");
            sb.append("回答：").append(qa.getAnswer()).append("\n");
            if (qa.getEvaluation() != null && !qa.getEvaluation().isBlank()) {
                sb.append("评估：").append(qa.getEvaluation()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}

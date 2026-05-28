package com.project.demo.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 评估 Agent：后台评估面试问答
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "jsonChatModel",
        chatMemoryProvider = "evaluateChatMemoryProvider",
        tools = "evaluateAgentTool"
)
public interface EvaluateAgent {

     @SystemMessage("""
    你是技术面试官，负责评估面试表现。
    
    模式判断：
    - 用户消息包含"【总评】"：进行总评。必须先调用 getInterviewHistoryByRecordId(recordId) 获取历史问答，再输出总评JSON
    - 其他：对单次问答评估，输出评估JSON
    
    单次评估JSON：
    {
      "考查知识点": "...",
      "评估": "..."
    }
    
    总评JSON：
    {
      "待提升知识点列表": [
        {"知识点名称": "...", "薄弱程度": "高/中/低"}
      ],
      "总评": "..."
    }
    
    规则：
    - 无效回答（空白、无意义字符、明确表示不会）：考查知识点留空，评估为"回答无效或无意义"
    - 回答跑题：指出相关性缺失
    - 明显错误：直接指出
    - 评估维度：准确性、完整性、深度、逻辑性、拓展性
    - 专业直接，不套话
    - 单次评估150字以内，总评200字以内
    """)
    String chat(@MemoryId Long memoryId, @UserMessage String userMessage);
}
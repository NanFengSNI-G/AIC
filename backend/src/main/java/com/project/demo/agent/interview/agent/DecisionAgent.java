package com.project.demo.agent.interview.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
    wiringMode = AiServiceWiringMode.EXPLICIT,
    chatModel = "openAiChatModel",
    chatMemoryProvider = "decisionChatMemoryProvider",
    tools = "decisionAgentTool"
)
public interface DecisionAgent {

    @SystemMessage("""
        你是面试流程的路由决策器。根据历史对话判断下一个面试阶段，调用对应的工具，然后以"OK"结束本轮。不要输出任何解释、不要扮演面试官说话、不要重复调用工具。

        ## 面试阶段（严格按顺序推进）
        1. opening  — 开场（第1轮必选，让候选人做自我介绍）
        2. tech     — 技术面试（2~3轮）
        3. project  — 项目深挖
        4. followup — 追问
        5. algorithm — 算法题（1~2轮）
        6. ending   — 结束

        ## 规则
        - 第1轮（无对话历史）调用 toOpening
        - 当前阶段可停留多轮
        - 候选人连续答不上来或主动要求结束，调用 toEnding
        """)
    String decide(@MemoryId Long memoryId, @UserMessage String context);
}

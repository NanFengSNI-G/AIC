package com.project.demo.agent.interview.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
    wiringMode = AiServiceWiringMode.EXPLICIT,
    chatModel = "openAiChatModel",
    streamingChatModel = "openAiStreamingChatModel",
    chatMemoryProvider = "interviewChatMemoryProvider"
)
public interface OpeningAgent {

    @SystemMessage("""
        你是一位专业的技术面试官。现在是面试开场阶段。

        ## 你的任务
        简短欢迎候选人，并请候选人做自我介绍。保持专业友好的语气。
        
        ## 当前候选人信息
        - 简历：{{resume}}
        - 岗位：{{jd}}
        """)
    Flux<String> chatStream(@MemoryId Long memoryId,
                            @V("resume") String resume,
                            @V("jd") String jd,
                            @UserMessage String userMessage);
}

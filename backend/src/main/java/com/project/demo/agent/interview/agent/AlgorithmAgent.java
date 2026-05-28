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
    chatMemoryProvider = "interviewChatMemoryProvider",
    contentRetriever = "contentRetriever"
)
public interface AlgorithmAgent {

    @SystemMessage("""
        你是一位专业的技术面试官。现在正在进行算法题考察环节。

        ## 你的任务
        请出一道算法题或场景设计题。题目描述要清晰，附带1-2个输入输出示例。只出一道题，不要一次提出多个问题。
        
        ## 候选人信息
        - 简历：{{resume}}
        - 岗位：{{jd}}
        """)
    Flux<String> chatStream(@MemoryId Long memoryId,
                            @V("resume") String resume,
                            @V("jd") String jd,
                            @UserMessage String userMessage);
}

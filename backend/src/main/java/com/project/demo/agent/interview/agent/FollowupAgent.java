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
public interface FollowupAgent {

    @SystemMessage("""
        你是一位专业的技术面试官。现在正在对候选人的上一轮回答进行深入追问。

        ## 你的任务
        请针对候选人在上一轮对话中的回答进行深入追问。问"怎么实现的"、"为什么这样设计"、"遇到过什么问题"等。
        只追问一个问题，语气专业友好。
        
        ## 候选人信息
        - 简历：{{resume}}
        - 岗位：{{jd}}
        """)
    Flux<String> chatStream(@MemoryId Long memoryId,
                            @V("resume") String resume,
                            @V("jd") String jd,
                            @UserMessage String userMessage);
}

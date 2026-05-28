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
public interface TechAgent {

    @SystemMessage("""
        你是一位专业的技术面试官。现在正在进行技术基础面试环节。

        ## 你的任务
        请基于候选人的简历和岗位JD中的技术栈，出一道技术基础问题。只出一道题，不要一次提出多个问题。
        候选人答不上来时，给一次提示；仍答不上就换个方向，不要纠缠。
        
        ## 候选人信息
        - 简历：{{resume}}
        - 岗位：{{jd}}
        """)
    Flux<String> chatStream(@MemoryId Long memoryId,
                            @V("resume") String resume,
                            @V("jd") String jd,
                            @UserMessage String userMessage);
}

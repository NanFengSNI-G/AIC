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
public interface ProjectAgent {

    @SystemMessage("""
        你是一位专业的技术面试官。现在正在进行项目经验深挖环节。

        ## 你的任务
        请针对候选人简历中的项目经验进行深挖，提问架构设计、技术选型、难点解决方案或团队协作等。
        只出一道题，不要一次提出多个问题。
        
        ## 候选人信息
        - 简历：{{resume}}
        - 岗位：{{jd}}
        """)
    Flux<String> chatStream(@MemoryId Long memoryId,
                            @V("resume") String resume,
                            @V("jd") String jd,
                            @UserMessage String userMessage);
}

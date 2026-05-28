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
public interface EndingAgent {

    @SystemMessage("""
        你是一位专业的技术面试官。面试即将结束。
        
        ## 你的任务
        请总结面试表现并给出简短祝福。最后一句必须说"面试到此结束"。
        """)
    Flux<String> chatStream(@MemoryId Long memoryId,
                            @V("resume") String resume,
                            @V("jd") String jd,
                            @UserMessage String userMessage);
}

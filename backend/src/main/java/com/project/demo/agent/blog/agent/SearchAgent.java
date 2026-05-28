package com.project.demo.agent.blog.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
    wiringMode = AiServiceWiringMode.EXPLICIT,
    chatModel = "openAiChatModel",
    chatMemoryProvider = "blogChatMemoryProvider",
    tools = "forumSearchTool"
)
public interface SearchAgent {

    @SystemMessage("""
    你是一个论坛搜索助手。帮助用户检索论坛内容并给出总结。

    ## 工作方式
    1. 分析用户的搜索意图，提取关键词
    2. 使用 searchForum 工具检索论坛帖子
    3. 如果首次检索结果不理想，调整关键词再次检索
    4. 基于检索结果给出结构化总结

    ## 总结格式
    - 先总结一共找到多少相关内容
    - 按相关性列出关键发现
    - 对每项用 1-2 句说明要点
    - 如果未找到结果，如实告知并建议调整搜索方向

    ## 要求
    - 基于检索结果回答，不编造
    - 中文回复，简洁清晰
    - 如果用户追问细节，继续使用工具检索或基于已有结果补充
    """)
    String search(@MemoryId long memoryId, @UserMessage String query);
}

package com.project.demo.agent.blog.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
    wiringMode = AiServiceWiringMode.EXPLICIT,
    chatModel = "openAiChatModel",
    tools = "blogWriterTool"
)
public interface ContentAgent {

    @SystemMessage("""
    你是一个资深技术博客作者。根据给定的大纲，撰写完整的博客正文。

    ## 写作规范
    - 按照大纲中的章节顺序，撰写所有章节的完整内容
    - 使用 Markdown 格式，以 # 标题 开头
    - 技术深度要够，不要泛泛科普
    - 语言：中文，每个章节 200-400 字
    - 搜索不到资料也要基于知识尽力写

    ## 重要：代码块标记
    - 如果你认为某个位置需要代码示例来说明，在需要代码的位置插入标记：
      <!-- NEEDS_CODE: 简短描述需要什么代码 -->
    - 例如：<!-- NEEDS_CODE: Redis缓存穿透的Java解决方案 -->

    ## 重要：配图标记
    - 如果你认为某个位置需要配图（架构图、流程图等），在需要配图的位置插入标记：
      <!-- NEEDS_IMAGE: 简短描述需要什么图 -->
    - 例如：<!-- NEEDS_IMAGE: 系统架构图，展示client→cache→db的层级关系 -->

    ## 输出
    输出完整的 Markdown 博客正文（含上述标记），不要输出大纲中未涉及的内容。
    """)
    String writeContent(@UserMessage String outline);
}

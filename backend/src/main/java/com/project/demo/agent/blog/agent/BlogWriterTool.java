package com.project.demo.agent.blog.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlogWriterTool {

    private final ObjectMapper objectMapper;
    private final WebSearchEngine webSearchEngine;

    private List<Map<String, String>> urlLibraryCache;

    // ═══════════════════════════════════════════════════════════════
    // Tool 1：获取网址库
    // ═══════════════════════════════════════════════════════════════
    @Tool("获取预配置的技术网址库，包含各技术领域的文档、博客、教程站点。" +
          "这些网站是搜索时的参考来源。")
    public String getUrlLibrary() {
        try {
            if (urlLibraryCache == null) {
                ClassPathResource resource = new ClassPathResource("url-library.json");
                try (InputStream is = resource.getInputStream()) {
                    urlLibraryCache = objectMapper.readValue(
                        is, new TypeReference<List<Map<String, String>>>() {});
                }
            }

            if (urlLibraryCache == null || urlLibraryCache.isEmpty()) {
                return "网址库为空，使用通用搜索即可";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("技术网址库（共").append(urlLibraryCache.size()).append("个）：\n\n");
            for (int i = 0; i < urlLibraryCache.size(); i++) {
                Map<String, String> entry = urlLibraryCache.get(i);
                sb.append(i + 1).append(". 【").append(entry.get("category")).append("】");
                sb.append(entry.get("name")).append("\n");
                sb.append("   URL: ").append(entry.get("url")).append("\n");
                sb.append("   说明: ").append(entry.get("description")).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("读取网址库失败", e);
            return "读取网址库失败：" + e.getMessage() + "，请直接用通用搜索";
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Tool 2：网页检索
    // ═══════════════════════════════════════════════════════════════
    @Tool("根据指定关键词进行网页搜索，返回文章标题、摘要和链接。" +
          "建议换不同角度和关键词搜索同一知识点 2-3 次。")
    public String searchWebForTopic(
            @P("搜索关键词，建议包含技术名称和搜索角度。" +
               "例如 'Redis缓存穿透 原理'、'Redis cache penetration best practice'")
            String keyword) {
        try {
            WebSearchResults results = webSearchEngine.search(
                WebSearchRequest.builder()
                    .searchTerms(keyword)
                    .maxResults(5)
                    .build()
            );

            if (results == null || results.results() == null || results.results().isEmpty()) {
                return "未找到与 '" + keyword + "' 相关的内容。建议换关键词或尝试英文搜索。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\"").append(keyword).append("\" 搜索结果（")
              .append(results.results().size()).append(" 条）：\n\n");

            for (int i = 0; i < results.results().size(); i++) {
                var r = results.results().get(i);
                sb.append(i + 1).append(". ").append(r.title()).append("\n");
                sb.append("   摘要：").append(r.snippet()).append("\n");
                if (r.url() != null) {
                    sb.append("   链接：").append(r.url()).append("\n");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("网页检索失败: keyword={}", keyword, e);
            return "网页检索失败：" + e.getMessage() + "，请换关键词重试。";
        }
    }

}

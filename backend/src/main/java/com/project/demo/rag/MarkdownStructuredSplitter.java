package com.project.demo.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 结构化分块器 —— 按标题层级切割，保护代码块完整性，附加富元数据。
 */
public class MarkdownStructuredSplitter implements DocumentSplitter {

    private static final int TARGET_MIN_CHARS = 300;
    private static final int TARGET_MAX_CHARS = 800;

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,4})\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");

    @Override
    public List<TextSegment> split(Document document) {
        String text = document.text();
        Metadata docMeta = document.metadata();
        String fileName = docMeta.getString("fileName");
        if (fileName == null) fileName = "unknown";

        // 保护代码块：先提取出来，用占位符替代，分块后再还原
        List<String> codeBlocks = new ArrayList<>();
        String protectedText = CODE_BLOCK_PATTERN.matcher(text).replaceAll(mr -> {
            codeBlocks.add(mr.group());
            return "%%CODEBLOCK_" + (codeBlocks.size() - 1) + "%%";
        });

        // 按标题层级切分
        List<Section> sections = splitByHeadings(protectedText, fileName);

        // 将过大的 section 按段落继续切分
        List<TextSegment> segments = new ArrayList<>();
        int chunkIndex = 0;
        for (Section section : sections) {
            if (section.content.length() <= TARGET_MAX_CHARS) {
                segments.add(buildSegment(section, fileName, chunkIndex++, codeBlocks));
            } else {
                segments.addAll(splitLargeSection(section, fileName, chunkIndex, codeBlocks));
                chunkIndex += segments.size();
            }
        }

        // 重新计算 chunk_index
        for (int i = 0; i < segments.size(); i++) {
            TextSegment seg = segments.get(i);
            Metadata meta = seg.metadata().copy();
            meta.put("chunk_index", String.valueOf(i));
            segments.set(i, TextSegment.from(seg.text(), meta));
        }

        return segments;
    }

    private List<Section> splitByHeadings(String text, String fileName) {
        List<Section> sections = new ArrayList<>();
        Matcher m = HEADING_PATTERN.matcher(text);

        int lastEnd = 0;
        String lastTitle = "概述";

        while (m.find()) {
            // 捕获上一个标题到当前标题之间的内容
            if (m.start() > lastEnd) {
                String content = text.substring(lastEnd, m.start()).trim();
                if (!content.isEmpty()) {
                    sections.add(new Section(lastTitle, content));
                }
            }
            lastTitle = m.group(2).trim();
            lastEnd = m.end();
        }

        // 最后一个 section
        if (lastEnd < text.length()) {
            String content = text.substring(lastEnd).trim();
            if (!content.isEmpty()) {
                sections.add(new Section(lastTitle, content));
            }
        } else if (sections.isEmpty() && !text.trim().isEmpty()) {
            sections.add(new Section(lastTitle, text.trim()));
        }

        return sections;
    }

    private List<TextSegment> splitLargeSection(Section section, String fileName,
                                                 int startIndex, List<String> codeBlocks) {
        List<TextSegment> result = new ArrayList<>();
        String content = section.content;

        // 按段落 (\n\n) 切分
        String[] paragraphs = content.split("\\n\\n");
        StringBuilder buffer = new StringBuilder();
        int idx = startIndex;

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            if (buffer.length() + trimmed.length() > TARGET_MAX_CHARS && buffer.length() >= TARGET_MIN_CHARS) {
                result.add(buildSegment(new Section(section.title, buffer.toString()), fileName, idx++, codeBlocks));
                buffer.setLength(0);
            }

            if (buffer.length() > 0) buffer.append("\n\n");
            buffer.append(trimmed);
        }

        if (buffer.length() > 0) {
            result.add(buildSegment(new Section(section.title, buffer.toString()), fileName, idx, codeBlocks));
        }

        return result;
    }

    private TextSegment buildSegment(Section section, String fileName, int chunkIndex,
                                      List<String> codeBlocks) {
        // 还原代码块
        String content = section.content;
        for (int i = 0; i < codeBlocks.size(); i++) {
            content = content.replace("%%CODEBLOCK_" + i + "%%", codeBlocks.get(i));
        }

        String contentType = content.contains("```") ? "code" : "text";

        Metadata meta = new Metadata()
                .put("source_file", fileName)
                .put("section_title", section.title)
                .put("chunk_index", String.valueOf(chunkIndex))
                .put("content_type", contentType);

        String enrichedText = String.format("[来源: %s / %s]\n%s", fileName, section.title, content);

        return TextSegment.from(enrichedText, meta);
    }

    private record Section(String title, String content) {}
}

package com.project.demo.agent.blog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.demo.dto.Outline;

public final class OutlineHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private OutlineHelper() {}

    public static String toJson(Outline outline) {
        try {
            return objectMapper.writeValueAsString(outline);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("大纲序列化失败", e);
        }
    }

    public static Outline fromJson(String json) {
        String cleaned = json != null ? json.trim() : "";
        // 剥掉 ```json ... ``` 外壳
        if (cleaned.startsWith("```")) {
            int start = cleaned.indexOf('\n');
            cleaned = start > 0 ? cleaned.substring(start + 1) : cleaned.substring(3);
            int end = cleaned.lastIndexOf("```");
            if (end > 0) cleaned = cleaned.substring(0, end);
            cleaned = cleaned.trim();
        }

        try {
            if (cleaned.startsWith("[")) {
                // 处理 ["{...}"] — LLM 把 JSON 对象包在数组的字符串里了
                try {
                    String[] strings = objectMapper.readValue(cleaned, String[].class);
                    if (strings.length > 0 && strings[0].startsWith("{")) {
                        cleaned = strings[0];
                        return objectMapper.readValue(cleaned, Outline.class);
                    }
                } catch (JsonProcessingException ignored) {
                    // 不是字符串数组，继续后续逻辑
                }

                // 尝试解析为 Outline[]（标准格式: [{title, sections}, ...]）
                try {
                    Outline[] arr = objectMapper.readValue(cleaned, Outline[].class);
                    if (arr.length > 0) return arr[0];
                    throw new RuntimeException("大纲数组为空");
                } catch (JsonProcessingException e) {
                    // LLM 可能直接返回了 sections 数组 [{heading, keyPoints}, ...]
                    // 尝试解析为 Section[]，然后包装成 Outline
                    if (e.getMessage() != null && e.getMessage().contains("heading")) {
                        Outline.Section[] sections = objectMapper.readValue(cleaned, Outline.Section[].class);
                        Outline outline = Outline.builder()
                            .title("技术博客")
                            .sections(java.util.List.of(sections))
                            .build();
                        return outline;
                    }
                    throw e;
                }
            }
            return objectMapper.readValue(cleaned, Outline.class);
        } catch (JsonProcessingException e) {
            String preview = cleaned.length() > 300 ? cleaned.substring(0, 300) + "..." : cleaned;
            throw new RuntimeException("大纲反序列化失败，输入预览: " + preview, e);
        }
    }
}

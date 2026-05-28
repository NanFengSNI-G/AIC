package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogAgentSSEEvent {
    private String type;        // intent, outline, content_progress, code_progress,
                                // review, publish_result, draft_result, error
    private String message;     // 用户可读的描述
    private String data;        // 负载数据（JSON字符串或纯文本）
}

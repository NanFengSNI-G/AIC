package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogAgentRequest {
    private String sessionId;   // 会话ID（必填，由 POST /api/blog/conversations 创建）
    private String message;
    private Long sectionId;
}

package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogAgentResumeRequest {
    private String sessionId;   // 会话ID（chat 返回的 sessionId）
    private String decision;    // PUBLISH or DRAFT
    private Long sectionId;     // 发布板块（PUBLISH时必填）
}

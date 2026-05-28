package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private Long friendId;
    private String friendUsername;
    private String friendAvatar;
    private String lastMsg;
    private Integer unread;
    private LocalDateTime updateTime;
}

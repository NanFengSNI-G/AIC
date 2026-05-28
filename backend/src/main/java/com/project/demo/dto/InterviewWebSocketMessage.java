package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 消息格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewWebSocketMessage {

    /**
     * 消息类型：START, MESSAGE, STREAM, STREAM_END, END, ERROR
     */
    private String type;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 问题
     */
    private String question;

    /**
     * TTS 音频数据 (base64 编码的 PCM)
     */
    private String audioBase64;

    /**
     * 音频序列号，前端用于排队播放
     */
    private Integer sequenceId;

    // 消息类型常量
    public static final String TYPE_AGENT_END = "AGENT_END";
    public static final String TYPE_START = "START";
    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_STREAM = "STREAM";
    public static final String TYPE_STREAM_END = "STREAM_END";
    public static final String TYPE_AUDIO_STREAM = "AUDIO_STREAM";
    public static final String TYPE_AUDIO_END = "AUDIO_END";
    public static final String TYPE_END = "END";
    public static final String TYPE_ERROR = "ERROR";
}

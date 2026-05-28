package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    /** 接收者 ID */
    private Long toUserId;

    /** 消息内容 */
    private String content;

    /** 消息类型: 1-文本 2-图片 3-文件 4-语音, 默认 1 */
    private Integer msgType = 1;
}

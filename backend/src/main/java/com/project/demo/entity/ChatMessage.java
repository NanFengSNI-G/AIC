package com.project.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息唯一标识（UUID），用于幂等去重 */
    private String messageId;

    /** 发送者ID */
    private Long fromUserId;

    /** 接收者ID */
    private Long toUserId;

    /** 消息内容 */
    private String content;

    /** 消息类型: 1-文本 2-图片 3-文件 4-语音 */
    private Integer msgType;

    /** 创建时间 */
    private LocalDateTime createTime;
}

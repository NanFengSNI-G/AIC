package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostSyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件类型: CREATE / UPDATE / DELETE */
    private String type;

    /** 帖子ID */
    private Long postId;

    public static ForumPostSyncMessage create(Long postId) {
        return new ForumPostSyncMessage("CREATE", postId);
    }

    public static ForumPostSyncMessage update(Long postId) {
        return new ForumPostSyncMessage("UPDATE", postId);
    }

    public static ForumPostSyncMessage delete(Long postId) {
        return new ForumPostSyncMessage("DELETE", postId);
    }
}

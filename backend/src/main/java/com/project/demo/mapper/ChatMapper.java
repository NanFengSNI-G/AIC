package com.project.demo.mapper;

import com.project.demo.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMapper {

    /** 插入单条消息 */
    int insert(ChatMessage msg);

    /** 分页查询与好友的聊天历史 */
    List<ChatMessage> selectHistory(@Param("userId") Long userId,
                                    @Param("friendId") Long friendId,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);
}

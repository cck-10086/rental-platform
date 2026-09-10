package com.rental.mapper;

import com.rental.entity.AiConversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {

    @Select("SELECT * FROM ai_conversation WHERE user_id = #{userId} AND conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<AiConversation> selectByConversationId(Long userId, String conversationId);
}

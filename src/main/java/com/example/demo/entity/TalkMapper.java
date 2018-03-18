package com.example.demo.entity;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Mapper
@Component(value = "talkMapper")
public interface TalkMapper {
    @Insert("insert into talk(talker_id, receiver_id, content, start_time, isHandle, group_id) values(#{talker_id},#{receiver_id},#{content},#{start_time},#{isHandle},#{group_id})")
    int talkRecord(@Param("talker_id") Long talker_id, @Param("receiver_id") Long receiver_id, @Param("content") String content, @Param("start_time") Date start_time, @Param("isHandle") boolean isHandle,@Param("group_id") String group_id);

    @Select("select * from talk where talker_id = #{talker_id} and receiver_id = #{receiver_id} and isHandle = 0 order by start_time")
    List<Talk> leaveWord(@Param("talker_id") Long talker_id, @Param("receiver_id") Long receiver_id);

    @Select("select talker_id from talk where receiver_id = #{receiver_id} and isHandle = 0 group by talker_id")
    List<Long> leaveWordSender(@Param("receiver_id") Long receiver_id);

    @Update("update talk set isHandle = 1 where receiver_id = #{receiver_id}")
    int changeStatus(@Param("receiver_id") Long receiver_id);
}

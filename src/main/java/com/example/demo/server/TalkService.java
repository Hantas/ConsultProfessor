package com.example.demo.server;

import com.example.demo.entity.Talk;
import com.example.demo.entity.TalkMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TalkService {
    @Autowired
    private TalkMapper mapper;

    public int talkRecode(Long talker_id, Long receiver_id, String content, Date time, boolean isHandle, String group_id) {
        return mapper.talkRecord(talker_id, receiver_id, content, time, isHandle, group_id);
    }

    public List<Talk> leaveWord(Long talker_id, Long receiver_id) {
        return mapper.leaveWord(talker_id, receiver_id);
    }

    public List<Long> leaveWordSender(Long receiver_id) {
        return mapper.leaveWordSender(receiver_id);
    }

    public void changeStatus(Long receiver_id) {
        mapper.changeStatus(receiver_id);
    }
}

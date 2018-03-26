package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.example.demo.entity.MsgBean;
import com.example.demo.entity.ResponseBean;
import com.example.demo.entity.Talk;
import com.example.demo.server.AccountService;
import com.example.demo.server.MyWebSocket;
import com.example.demo.server.TalkService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@MapperScan("com.example.demo.entity")
@Controller
public class TalkController {

    @Autowired
    TalkService talkService;
    @Autowired
    AccountService accountService;

    @PostMapping("/getLeaveWord")
    @ResponseBody
    public List<String> getLeaveWord(String receiver_id){
        List<Long> talker_ids = talkService.leaveWordSender(Long.valueOf(receiver_id));
        if (talker_ids != null){
            List<String> wordList = new ArrayList<>();
            for (Long talker_id : talker_ids){
                List<Talk> talks = talkService.leaveWord(Long.valueOf(talker_id), Long.valueOf(receiver_id));
                String name = accountService.getName(talker_id);
                for (Talk talk: talks){
                    String meta = JSON.toJSONString(new ResponseBean(processing(name, talk.getContent(), talk.getStart_time()), "build", talker_id.toString()));
                }
            }
            talkService.changeStatus(Long.valueOf(receiver_id));
            return wordList;
        }
        return null;
    }

    @PostMapping("/getGroup")
    @ResponseBody
    public String getGroup(String user_id){
        if (MyWebSocket.getAccountMap().containsKey(user_id)){
            String group_id = new ArrayList<>(MyWebSocket.getAccountMap().get(user_id)).get(0);
            return group_id;
        }
        return null;
    }

    private String processing(String name, String message, Date date) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        message = format.format(date) + "</br>" + name + ":" + message + "</br>";
        return message;
    }
}

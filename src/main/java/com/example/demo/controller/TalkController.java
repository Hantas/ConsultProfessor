package com.example.demo.controller;

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
                StringBuffer words = new StringBuffer();
                List<Talk> talks = talkService.leaveWord(Long.valueOf(talker_id), Long.valueOf(receiver_id));
                String name = accountService.getName(talker_id);
                words.append("system;");
                for (Talk talk: talks){
                    words.append(processing(name, talk.getContent(), talk.getStart_time()));
                }
                words.append(";" + talker_id + ";build");
                wordList.add(words.toString());
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
            List<String> list = new ArrayList<>(MyWebSocket.getAccountMap().get(user_id));
            String group_id = list.get(0);
            String to = MyWebSocket.getChatMap().get(group_id);
            return group_id + "&" + to;
        }
        return null;
    }

    private String processing(String name, String message, Date date) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        message = format.format(date) + "</br>" + name + ":" + message + "</br>";
        return message;
    }
}

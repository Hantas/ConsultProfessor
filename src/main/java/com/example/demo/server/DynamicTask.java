package com.example.demo.server;

import com.alibaba.fastjson.JSON;
import com.example.demo.entity.ResponseBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Service
public class DynamicTask {

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;
    private String talk_id;
    private ScheduledFuture<?> future;

    public void setTalk_id(String talk_id) {
        this.talk_id = talk_id;
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    public String startCron() {
        stopCron();
        System.out.println("new " + new Date());
        Runnable runnable = new MyRunnable();
        Trigger trigger = new PeriodicTrigger(0);
        ((PeriodicTrigger) trigger).setInitialDelay(43200000);
        future = threadPoolTaskScheduler.schedule(runnable, trigger);
        return "startCron";
    }

    public void stopCron() {
        if (future != null) {
            future.cancel(true);
        }
    }

    private class MyRunnable implements Runnable {
        @Override
        public void run() {
            System.out.println("talk stop " + new Date());
            Map<String, List<String>> chatMap = MyWebSocket.getChatMap();
            Map<String, Set<String>> accountMap = MyWebSocket.getAccountMap();
            Map<String, MyWebSocket> socketMap = WebSocketUtils.getMap();
            List<String> members = new ArrayList<>(chatMap.get(talk_id));
            for (int i = 0; i < members.size(); i++) {
                MyWebSocket socket = socketMap.get(members.get(i));
                if (socket != null) {
                    socket.getSession().getAsyncRemote().sendText(JSON.toJSONString(new ResponseBean(null, "disband", talk_id)));
                }
                accountMap.get(members.get(i)).remove(talk_id);
                if (accountMap.get(members.get(i)).size() == 0)
                    accountMap.remove(members.get(i));
            }
            chatMap.remove(talk_id);
            System.out.println(accountMap + " " + chatMap);
            stopCron();
        }
    }

}
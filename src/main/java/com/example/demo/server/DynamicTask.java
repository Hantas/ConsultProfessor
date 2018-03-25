package com.example.demo.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
        ((PeriodicTrigger) trigger).setInitialDelay(10000);
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
            Map<String, String> chatMap = MyWebSocket.getChatMap();
            Map<String, Set<String>> accountMap = MyWebSocket.getAccountMap();
            Map<String, MyWebSocket> socketMap = WebSocketUtils.getMap();
            String memberStr = chatMap.get(talk_id);
            String[] members = memberStr.split(";");
            for (int i = 0; i < members.length; i++) {
                MyWebSocket socket = socketMap.get(members[i]);
                if (socket != null) {
                    socket.getSession().getAsyncRemote().sendText("system;" + talk_id + ";disband");
                }
                accountMap.get(members[i]).remove(talk_id);
                if (accountMap.get(members[i]).size() == 0)
                    accountMap.remove(members[i]);
            }
            chatMap.remove(talk_id);
            System.out.println(accountMap + " " + chatMap);
            stopCron();
        }
    }

}
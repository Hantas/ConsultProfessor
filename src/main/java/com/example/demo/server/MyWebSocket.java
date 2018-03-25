package com.example.demo.server;

import com.alibaba.fastjson.JSON;
import com.example.demo.entity.MsgBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by 10742 on 2018/2/7.
 */
@Component
@ServerEndpoint(value = "/websocket/{user_id}")
public class MyWebSocket {

    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;
    private static ApplicationContext act;
    private static TalkService talkService;
    //记录会话id与群聊成员id的映射关系
    private static Map<String, String> chatMap = new ConcurrentHashMap<String, String>();
    private static Map<String, DynamicTask> taskMap = new ConcurrentHashMap<String, DynamicTask>();
    //记录用户id与其所处所有群聊中的会话id的映射关系
    private static Map<String, Set<String>> accountMap = new ConcurrentHashMap<String, Set<String>>();

    static {
        act = ApplicationContextRegister.getApplicationContext();
        talkService = act.getBean(TalkService.class);
    }

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;
    private boolean isTalked;
    private MsgBean msgBean;

    public Session getSession() {
        return session;
    }

    public static Map<String, String> getChatMap() {
        return chatMap;
    }

    public static Map<String, Set<String>> getAccountMap() {
        return accountMap;
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(@PathParam("user_id") String user_id, Session session) {
        this.session = session;
        this.isTalked = false;
        WebSocketUtils.put(user_id, this);
        addOnlineCount();           //在线数加1
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(@PathParam("user_id") String user_id) throws IOException {
        WebSocketUtils.remove(user_id);
        subOnlineCount();           //在线数减1
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        System.out.println(message);
        msgBean = JSON.parseObject(message, MsgBean.class);
        sendMessage(msgBean.getMessage(), msgBean.getRole(), msgBean.getUser_id(), msgBean.getName(), msgBean.getTo(), msgBean.getMode(), msgBean.getTalk_id());
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    /**
     * @param mode 0:服务器转发信息 1：发起建立群聊的信号 2：群聊 3:退出群聊
     */
    private void sendMessage(String message, String role, String user_id, String name, String to, String mode, String talk_id) throws IOException {
        if (mode.equals("0")) {
            MyWebSocket item = WebSocketUtils.get(to);
            if (role.equals("common")) {
                Date date = new Date();
                if (item == null)
                    record(user_id, to, message, date, false, null);
                else if (!isTalked) {
                    isTalked = true;
                    record(user_id, to, message, date, true, null);
                    item.getSession().getAsyncRemote().sendText("system;" + processing(name, message, date) + ";" + user_id + ";build");
                } else {
                    record(user_id, to, message, date, true, null);
                    item.getSession().getAsyncRemote().sendText("personal;" + processing(name, message, date) + ";" + user_id);
                }
                this.getSession().getAsyncRemote().sendText("personal;" + processing(name, message, date));
            } else {
                Date date = new Date();
                if (item == null)
                    record(user_id, to, message, date, false, null);
                else {
                    if (accountMap.containsKey(to))
                        this.getSession().getAsyncRemote().sendText("personal;用户正在群聊中;" + to);
                    else {
                        record(user_id, to, message, date, true, null);
                        item.getSession().getAsyncRemote().sendText("personal;" + processing(name, message, date));
                        this.getSession().getAsyncRemote().sendText("personal;" + processing(name, message, date) + ";" + to);
                    }
                }
            }
        } else if (mode.equals("1")) {
            String[] list = to.split(";");
            DynamicTask task = act.getBean(DynamicTask.class);
            task.setTalk_id(talk_id);
            chatMap.put(talk_id, to);
            taskMap.put(talk_id, task);
            task.startCron();
            for (int i = 0; i < list.length; i++) {
                MyWebSocket account = WebSocketUtils.get(list[i]);
                if (account != null) {
                    if (!accountMap.containsKey(list[i]))
                        accountMap.put(list[i], new HashSet<>());
                    if (!accountMap.get(list[i]).contains(talk_id))
                        accountMap.get(list[i]).add(talk_id);
                    account.getSession().getAsyncRemote().sendText("system;" + talk_id + ";" + to + ";rebuild");
                }
            }
            System.out.println(accountMap);
            System.out.println(chatMap);
        } else if (mode.equals("2")) {
            Date date = new Date();
            taskMap.get(talk_id).startCron();
            String[] list = to.split(";");
            for (int i = 0; i < list.length; i++) {
                MyWebSocket account = WebSocketUtils.get(list[i]);
                if (account != null)
                    account.getSession().getAsyncRemote().sendText("groupChat;" + processing(name, message, date) + ";" + talk_id);
            }
            record(user_id, "0", message, date, true, talk_id);
        } else if (mode.equals("3")) {
            String memberStr = chatMap.get(talk_id);
            String[] members = memberStr.split(";");
            if (role.equals("common")) {
                memberStr = memberStr.substring(0, memberStr.length() - members[members.length - 1].length() - 1);
                for (int i = 0; i < members.length - 1; i++) {
                    MyWebSocket account = WebSocketUtils.get(members[i]);
                    if (account != null)
                        account.getSession().getAsyncRemote().sendText("system;客户已退出群聊;" + talk_id + ";" + memberStr + ";exit");
                }
                members = memberStr.split(";");
                chatMap.put(talk_id, memberStr);
                accountMap.remove(user_id);
            } else {
                StringBuilder newMemberStr = new StringBuilder();
                for (int i = 0; i < members.length; i++) {
                    if (!members[i].equals(user_id))
                        newMemberStr.append(members[i] + ";");
                }
                memberStr = newMemberStr.toString().substring(0, memberStr.length() - 1);
                members = memberStr.split(";");
                accountMap.get(user_id).remove(talk_id);
                if (accountMap.get(user_id).size() == 0)
                    accountMap.remove(user_id);
                if (members.length > 1) {
                    chatMap.put(talk_id, memberStr);
                    for (int i = 0; i < members.length; i++) {
                        MyWebSocket account = WebSocketUtils.get(members[i]);
                        if (account != null)
                            account.getSession().getAsyncRemote().sendText("system;none;" + talk_id + ";" + memberStr + ";exit");
                    }
                }
            }
            if (members.length <= 1) {
                MyWebSocket account = WebSocketUtils.get(members[0]);
                if (account != null) {
                    account.getSession().getAsyncRemote().sendText("system;" + talk_id + ";disband");
                }
                accountMap.get(members[0]).remove(talk_id);
                if (accountMap.get(members[0]).size() == 0)
                    accountMap.remove(members[0]);
                chatMap.remove(talk_id);
            }
            System.out.println(accountMap);
            System.out.println(chatMap);
        }
    }

    /**
     * 聊天内容记录
     */
    private void record(String talker_id, String receiver_id, String message, Date time, boolean isHandle, String group_id) {
        talkService.talkRecode(Long.valueOf(talker_id), Long.valueOf(receiver_id), message, time, isHandle, group_id);
    }

    /**
     * 聊天内容格式化
     */
    private String processing(String name, String message, Date date) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        message = format.format(date) + "</br>" + name + ":" + message;
        return message;
    }

    /**
     * 服务器发送自定义消息
     */
    private void sendInfo() throws IOException {
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        MyWebSocket.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        MyWebSocket.onlineCount--;
    }
}

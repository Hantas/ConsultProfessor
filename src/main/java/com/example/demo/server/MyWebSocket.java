package com.example.demo.server;

import com.alibaba.fastjson.JSON;
import com.example.demo.entity.MsgBean;
import com.example.demo.entity.ResponseBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

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
    private static Map<String, List<String>> chatMap = new ConcurrentHashMap<String, List<String>>();
    //记录用户id与其所处所有群聊中的会话id的映射关系
    private static Map<String, Set<String>> accountMap = new ConcurrentHashMap<String, Set<String>>();
    private static Map<String, DynamicTask> taskMap = new ConcurrentHashMap<String, DynamicTask>();

    static {
        act = ApplicationContextRegister.getApplicationContext();
        talkService = act.getBean(TalkService.class);
    }

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;
    private boolean isTalked;

    public Session getSession() {
        return session;
    }

    public static Map<String, List<String>> getChatMap() {
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
        MsgBean msgBean = JSON.parseObject(message, MsgBean.class);
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
    private void sendMessage(String message, String role, String user_id, String name, String[] tos, String mode, String talk_id) throws IOException {
        List<String> to = null;
        if (tos != null)
            to = new ArrayList<>(Arrays.asList(tos));
        if (mode.equals("0")) {
            MyWebSocket item = WebSocketUtils.get(to.get(0));
            if (role.equals("common")) {
                Date date = new Date();
                if (item == null)
                    record(user_id, to.get(0), message, date, false, null);
                else {
                    if (!isTalked) {
                        isTalked = true;
                        item.getSession().getAsyncRemote().sendText(JSON.toJSONString(new ResponseBean(processing(name, message, date), "build", user_id)));
                    } else {
                        item.getSession().getAsyncRemote().sendText(JSON.toJSONString(new ResponseBean(processing(name, message, date), null, user_id)));
                    }
                    record(user_id, to.get(0), message, date, true, null);
                }
                this.getSession().getAsyncRemote().sendText(JSON.toJSONString(new ResponseBean(processing(name, message, date), null, null)));
            } else {
                Date date = new Date();
                if (item == null)
                    record(user_id, to.get(0), message, date, false, null);
                else {
                    if (accountMap.containsKey(to.get(0)))
                        this.getSession().getAsyncRemote().sendText(JSON.toJSONString(new ResponseBean("用户正在群聊中", null, to.get(0))));
                    else {
                        record(user_id, to.get(0), message, date, true, null);
                        item.getSession().getAsyncRemote().sendText(JSON.toJSONString(new ResponseBean(processing(name, message, date), null, null)));
                        this.getSession().getAsyncRemote().sendText(JSON.toJSONString(new ResponseBean(processing(name, message, date), null, to.get(0))));
                    }
                }
            }
        } else if (mode.equals("1")) {
            DynamicTask task = act.getBean(DynamicTask.class);
            task.setTalk_id(talk_id);
            chatMap.put(talk_id, to);
            taskMap.put(talk_id, task);
            task.startCron();
            for (int i = 0; i < to.size(); i++) {
                MyWebSocket account = WebSocketUtils.get(to.get(i));
                if (!accountMap.containsKey(to.get(i)))
                    accountMap.put(to.get(i), new HashSet<>());
                if (!accountMap.get(to.get(i)).contains(talk_id))
                    accountMap.get(to.get(i)).add(talk_id);
                if (account != null) {
                    account.getSession().getAsyncRemote().sendText(JSON.toJSONString(new ResponseBean(null, "fresh", talk_id)));
                }
            }
            System.out.println(accountMap);
            System.out.println(chatMap);
        } else if (mode.equals("2")) {
            Date date = new Date();
            taskMap.get(talk_id).startCron();
            List<String> list = new ArrayList(chatMap.get(talk_id));
            for (int i = 0; i < list.size(); i++) {
                MyWebSocket account = WebSocketUtils.get(list.get(i));
                if (account != null)
                    account.getSession().getAsyncRemote().sendText(JSON.toJSONString(new ResponseBean(processing(name, message, date), "groupChat", talk_id)));
            }
            record(user_id, "0", message, date, true, talk_id);
        } else if (mode.equals("3")) {
            List<String> members = new ArrayList<>(chatMap.get(talk_id));
            members.remove(user_id);
            if (members.size() == 1) {
                MyWebSocket account = WebSocketUtils.get(members.get(0));
                if (account != null) {
                    account.getSession().getAsyncRemote().sendText(JSON.toJSONString(new ResponseBean(null, "disband", talk_id)));
                }
                chatMap.remove(talk_id);
            } else {
                for (int i = 0; i < members.size(); i++) {
                    MyWebSocket account = WebSocketUtils.get(members.get(i));
                    if (account != null && role.equals("common"))
                        account.getSession().getAsyncRemote().sendText(JSON.toJSONString(new ResponseBean("客户退出群聊", "exit", talk_id)));
                    else if (account != null && role.equals("professor"))
                        account.getSession().getAsyncRemote().sendText(JSON.toJSONString(new ResponseBean("XXX专家退出", "exit", talk_id)));
                }
                chatMap.put(talk_id, members);
            }
            accountMap.get(user_id).remove(talk_id);
            if (accountMap.get(user_id).size() == 0)
                accountMap.remove(user_id);
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

package com.example.demo.server;

import com.alibaba.fastjson.JSON;
import com.example.demo.entity.Account;
import com.example.demo.entity.MsgBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by 10742 on 2018/2/7.
 */
@Component
@ServerEndpoint(value = "/websocket/{user_id}")
public class MyWebSocket {

    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;
    private static TalkService talkService;
    //记录客户id与群聊成员id的映射关系
    private static Map<String, String> chatMap = new ConcurrentHashMap<String, String>();
    //记录专家id与其所处所有群聊中的客户的id的映射关系
    private static Map<String, List<String>> professorMap = new ConcurrentHashMap<String, List<String>>();
    //记录专家所处群聊中所有客户的id
    //private List<String> relationList = Collections.synchronizedList(new ArrayList<>());

    static {
        ApplicationContext act = ApplicationContextRegister.getApplicationContext();
        talkService = act.getBean(TalkService.class);
    }

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;
    private boolean isTalked;
    private MsgBean msgBean;

    public Session getSession() {
        return session;
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
        //webSocketSet.remove(this);  //从set中删除
        WebSocketUtils.remove(user_id);
        if (msgBean != null && msgBean.getRole().equals("common"))
            sendInfo();
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
        sendMessage(msgBean.getMessage(), msgBean.getRole(), msgBean.getUser_id(), msgBean.getName(), msgBean.getTo(), msgBean.getMode());
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
    private void sendMessage(String message, String role, String user_id, String name, String to, String mode) throws IOException {
        if (mode.equals("0")) {
            MyWebSocket item = WebSocketUtils.get(to);
            if (role.equals("common")) {
                Date date = new Date();
                if (item == null)
                    record(user_id, to, message, date, false);
                else if (!isTalked) {
                    isTalked = true;
                    record(user_id, to, message, date, true);
                    item.getSession().getAsyncRemote().sendText("system;" + processing(name, message, date) + ";" + user_id + ";build");
                } else {
                    record(user_id, to, message, date, true);
                    item.getSession().getAsyncRemote().sendText("personal;" + processing(name, message, date) + ";" + user_id);
                }
                this.getSession().getAsyncRemote().sendText("personal;" + processing(name, message, date));
            } else {
                Date date = new Date();
                if (item == null)
                    record(user_id, to, message, date, false);
                else {
                    if (chatMap.containsKey(to))
                        this.getSession().getAsyncRemote().sendText("personal;用户正在群聊中;" + to);
                    else {
                        record(user_id, to, message, date, true);
                        item.getSession().getAsyncRemote().sendText("personal;" + processing(name, message, date));
                        this.getSession().getAsyncRemote().sendText("personal;" + processing(name, message, date) + ";" + to);
                    }
                }
            }
        } else if (mode.equals("1")) {
            String[] list = to.split(";");
            chatMap.put(list[list.length - 1], to);
            for (int i = 0; i < list.length; i++) {
                System.out.println(list[i]);
                MyWebSocket account = WebSocketUtils.get(list[i]);
                if (account != null) {
                    if (i != list.length - 1 && !professorMap.get(list[i]).contains(list[list.length - 1]))
                        professorMap.get(list[i]).add(list[list.length - 1]);
                    account.getSession().getAsyncRemote().sendText("system;" + to + ";rebuild");
                }
            }
        } else if (mode.equals("2")) {
            Date date = new Date();
            String[] list = to.split(";");
            for (int i = 0; i < list.length; i++) {
                MyWebSocket account = WebSocketUtils.get(list[i]);
                account.getSession().getAsyncRemote().sendText("groupChat;" + processing(name, message, date) + ";" + list[list.length - 1] + "group");
            }
        } else if (mode.equals("3")) {
            to = to.substring(0, to.length() - 5);
            professorMap.get(user_id).remove(to);
            String chatersIdStr = chatMap.get(to);
            String[] chatersId = chatersIdStr.split(";");
            StringBuffer newChatersIdStr = new StringBuffer();
            for (int i = 0; i < chatersId.length; i++) {
                if (!chatersId[i].equals(user_id))
                    newChatersIdStr.append(chatersId[i] + ";");
            }
            chatersId = newChatersIdStr.toString().split(";");
            if (chatersId.length == 1 && chatersId[0].equals(to)) {
                MyWebSocket account = WebSocketUtils.get(chatersId[0]);
                if (account != null)
                    account.getSession().getAsyncRemote().sendText("system;disband");
                chatMap.remove(to);
            } else {
                chatMap.put(to, newChatersIdStr.toString());
                for (int j = 0; j < chatersId.length; j++) {
                    MyWebSocket account = WebSocketUtils.get(chatersId[j]);
                    if (account != null)
                        account.getSession().getAsyncRemote().sendText("system;" + newChatersIdStr.toString() + "exit");
                }
            }
        }
        //this.session.getAsyncRemote().sendText(message);
    }

    /**
     * 聊天内容记录
     */
    private void record(String talker_id, String receiver_id, String message, Date time, boolean isHandle) {
        talkService.talkRecode(Long.valueOf(talker_id), Long.valueOf(receiver_id), message, time, isHandle);
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
        if (chatMap.containsKey(msgBean.getUser_id())) {
            String[] list = chatMap.get(msgBean.getUser_id()).split(";");
            for (int i = 0; i < list.length - 1; i++) {
                MyWebSocket account = WebSocketUtils.get(list[i]);
                if (account != null)
                    account.getSession().getAsyncRemote().sendText("groupChat;用户已经下线;" + msgBean.getUser_id() + "group");
            }
            //主动退出群聊或者群聊活跃度低时才算真正意义上的退出群聊
            //chatMap.remove(msgBean.getUser_id());
        } else {
            MyWebSocket item = WebSocketUtils.get(msgBean.getTo());
            System.out.println(item + " " + msgBean.getMode() + " " + isTalked);
            if (item != null && msgBean.getMode().equals("0"))
                item.getSession().getAsyncRemote().sendText("personal;用户已经下线;" + msgBean.getUser_id());
        }
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

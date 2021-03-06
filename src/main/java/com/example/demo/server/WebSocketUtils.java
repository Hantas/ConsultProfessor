package com.example.demo.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketUtils {
    private static Map<String, MyWebSocket> map = new ConcurrentHashMap<String, MyWebSocket>();

    public static Map<String, MyWebSocket> getMap(){
        return map;
    }

    public static void put(String user_id, MyWebSocket session) {
        map.put(getKey(user_id), session);
    }

    public static MyWebSocket get(String user_id) {
        return map.get(getKey(user_id));

    }

    public static void remove(String user_id) {
        map.remove(getKey(user_id));
    }

    public static boolean hasConnection(String user_id) {
        return map.containsKey(getKey(user_id));
    }

    private static String getKey(String user_id) {
        return user_id;
    }

}

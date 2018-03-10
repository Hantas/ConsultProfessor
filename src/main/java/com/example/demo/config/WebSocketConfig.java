package com.example.demo.config;

import com.example.demo.server.TalkService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * Created by 10742 on 2018/2/7.
 */
@Configuration
@ComponentScan("com.example.demo.server")
public class WebSocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
    @Bean
    public TalkService talkService(){return new TalkService();}
}

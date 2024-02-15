package com.go.server.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import com.go.server.constance.MessageTypeEnum;
import com.go.server.response.MessageResponse;
import com.go.server.websocket.WebSocketHandler;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BroadcastController {

    private final WebSocketHandler messagingTemplate;
    
    @Scheduled(fixedDelay = 1000)
    public void scheduleTimeZone() {
        SimpleDateFormat newFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss",Locale.US);
        MessageResponse notification = MessageResponse.builder()
                                                      .messageType(MessageTypeEnum.SYSTEM)
                                                      .message(newFormat.format(new Date()))
                                                      .build();
        messagingTemplate.broadcast(notification);
    }
}

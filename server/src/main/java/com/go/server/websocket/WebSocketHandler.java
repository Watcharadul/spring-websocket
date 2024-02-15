package com.go.server.websocket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.go.server.constance.MessageTypeEnum;
import com.go.server.response.MessageResponse;
import com.google.gson.Gson;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private List<SesstionData> sessions = CustomSession.getInstance().getSessions();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        List<String> list = session.getHandshakeHeaders().get("token");
        if(list == null || list.isEmpty()){
            session.close();
            return;
        }
        
        SesstionData data = SesstionData.builder().id(session.getId()).name(list.get(0)).session(session).build();
        sessions.add(data);
        
        MessageResponse notification = MessageResponse.builder()
                                                      .messageType(MessageTypeEnum.PUBLIC)
                                                      .message("User joined: " + data.getName())
                                                      .data(sessions)
                                                      .build();
        broadcast(notification);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        MessageResponse output = new Gson().fromJson(message.getPayload(), MessageResponse.class);
        output.setMessageType(MessageTypeEnum.PUBLIC);
        if(output.getTo() == null || output.getTo().isEmpty()){
            broadcast(output);
            return;
        }
        sendMessageToClient(output);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        removeSessionById(session.getId(),response->{
            MessageResponse notification = MessageResponse.builder()
                                                          .messageType(MessageTypeEnum.PUBLIC)
                                                          .message("Logout : " + response.getName())
                                                          .data(sessions)
                                                          .build();
            broadcast(notification);
        });
    }

    private void removeSessionById(String idToRemove,Consumer<SesstionData> consumer) {
        Iterator<SesstionData> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            SesstionData sessionData = iterator.next();
            if (sessionData.getId().equals(idToRemove)) {
                iterator.remove();
                consumer.accept(sessionData);
            }
        }
    }

    public void broadcast(MessageResponse result){
        try{
            for (SesstionData session : sessions) {
                session.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(result)));
            }
        }catch(Exception ex){
            log.info(ex.getMessage());
        }
    }

    public void sendMessageToClient(MessageResponse result){
        try{
            for (SesstionData session : sessions) {
                if (session.getId().equals(result.getTo())) {
                    session.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(result)));
                    break;
                }
            }
        }catch(Exception ex){
            log.info(ex.getMessage());
        }
    }
}

@Data
class CustomSession {
    private static CustomSession instance = null;
    private List<SesstionData> sessions = new ArrayList<>();
    
    public static CustomSession getInstance(){
        if(instance == null){
            instance = new CustomSession();
        }
        return instance;
    }
}

@Data
@Builder
class SesstionData{
    private String id;
    private String name;
    
    @JsonIgnore
    private WebSocketSession session;
}

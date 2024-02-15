package com.go.client.socket;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.go.client.constance.ProcessStatusEnum;
import com.go.client.response.MessageResponse;
import com.google.gson.Gson;

public class SocketHandler extends Endpoint{
    
    private static SocketHandler instance;
    private Map<ProcessStatusEnum,SocketHandler.Listener> map = new EnumMap<>(ProcessStatusEnum.class);
    private WebSocketContainer container = null;
    private Session userSession = null;
    private String clientName;
    private String hostName;
    
    public interface Listener {
        void execute(Object object);
    }

    public void call(ProcessStatusEnum key,Object result){
        if(this.map.containsKey(key)){
            this.map.get(key).execute(result);
        }
    }
    
    public SocketHandler put(ProcessStatusEnum key , SocketHandler.Listener value){
        this.map.putIfAbsent(key, value);
        return this;
    }

    public SocketHandler(){
        container = ContainerProvider.getWebSocketContainer();
    }

    public static SocketHandler getInstance(){
        if(instance == null){
            instance = new SocketHandler();
        }
        return instance ;
    }

    public SocketHandler setHostName(String host){
        this.hostName = host;
        return this;
    }

    public SocketHandler setName(String name){
        this.clientName = name;
        return this;
    }

    public boolean isOpen(){
        if(this.userSession == null){
            return false;
        }
        return this.userSession.isOpen();
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpoint) {
        this.userSession = session;
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                MessageResponse output = new Gson().fromJson(message, MessageResponse.class);
                call(ProcessStatusEnum.MESSAGE,output);
            }
        });
        call(ProcessStatusEnum.ACTION,clientName);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        call(ProcessStatusEnum.ACTION,null);
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        call(ProcessStatusEnum.ACTION,throwable.getMessage());
    }

    public SocketHandler connect(){
        CompletableFuture.runAsync(() -> {
            try {
                /* ========== ... ========== */
                ClientEndpointConfig.Builder configBuilder = ClientEndpointConfig.Builder.create();
                ClientEndpointConfig clientConfig = configBuilder.configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void beforeRequest(Map<String, List<String>> headers) {
                        headers.put("token", Arrays.asList(clientName));
                    }
                }).build();
                /* ========== ... ========== */
                container.connectToServer(this,clientConfig,new URI("ws://"+hostName+"/ws"));
            } catch (Exception ex) {
                ex.printStackTrace();
                call(ProcessStatusEnum.ACTION,ex.getMessage());
            }
        });
        return this;
    }

    public void sendMessage(String to,String message){
        if (userSession != null && userSession.isOpen()) {
            try {
                MessageResponse request = MessageResponse.builder().form(userSession.getId()).to(to).message(message).build();
                userSession.getBasicRemote().sendText(new Gson().toJson(request));
            } catch (IOException ex) {
                call(ProcessStatusEnum.ACTION,ex.getMessage());
            }
        }
    }

    public void disconnect(){
        CompletableFuture.runAsync(() -> {
            try{
                if(userSession != null){
                    userSession.close();
                }
            }catch(Exception ex){
                call(ProcessStatusEnum.ACTION,ex.getMessage());
            }
        });
    }
}
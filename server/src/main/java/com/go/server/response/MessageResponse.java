package com.go.server.response;

import com.go.server.constance.MessageTypeEnum;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageResponse {
    private String form;
    private String to;
    private String message;
    private Object data;
    private MessageTypeEnum messageType;
}
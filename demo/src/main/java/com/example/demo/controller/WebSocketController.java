package com.example.demo.controller;


import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.enums.MessageType;
import com.example.demo.enums.UserStatus;
import com.example.demo.service.chat.ChatService;
import com.example.demo.service.user.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messaging;
    private final ChatService chatService;
    private final UserService userService;

    @MessageMapping("/chat/{roomId}/send")
    public void send(@DestinationVariable Long roomId,
                     @Payload SendPayload payload,
                     Principal principal) {

        if (principal == null || isBlank(payload.getContent())) return;

        User sender = userService.getByUsername(principal.getName());
        Message saved  = chatService.saveMessage(
                roomId, sender, payload.getContent(), MessageType.TEXT);

        // HashMap avoids Map.of() 10-entry limit
        Map<String, Object> msg = new HashMap<>();
        msg.put("id",             saved.getId());
        msg.put("content",        saved.getContent());
        msg.put("sentAt",         saved.getSentAt().toString());
        msg.put("senderName",     sender.getDisplayName());
        msg.put("senderUsername", sender.getUsername()); // JS uses this for isOwn detection
        msg.put("senderColor",    sender.getAvatarColor() != null
                ? sender.getAvatarColor() : "#4ECDC4");

        messaging.convertAndSend("/topic/room/" + roomId, msg);
    }

    @MessageMapping("/typing/{roomId}")
    public void typing(@DestinationVariable Long roomId,
                       @Payload TypingPayload payload,
                       Principal principal) {
        if (principal == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("username", principal.getName());
        msg.put("typing",   payload.isTyping());

        messaging.convertAndSend("/topic/room/" + roomId + "/typing", msg);
    }

    @MessageMapping("/presence")
    public void presence(@Payload PresencePayload payload, Principal principal) {
        if (principal == null) return;

        UserStatus status = "ONLINE".equals(payload.getStatus())
                ? UserStatus.ONLINE : UserStatus.OFFLINE;
        userService.setStatus(principal.getName(), status);

        Map<String, Object> msg = new HashMap<>();
        msg.put("username", principal.getName());
        msg.put("status",   status.name());

        messaging.convertAndSend("/topic/presence", msg);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    @Data
    public static class SendPayload     { private String content; }
    @Data
    public static class TypingPayload   { private boolean typing; }
    @Data
    public static class PresencePayload { private String status; }
}
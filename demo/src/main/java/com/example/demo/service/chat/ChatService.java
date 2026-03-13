package com.example.demo.service.chat;

import com.example.demo.entity.ChatRoom;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.enums.MessageType;
import com.example.demo.repository.ChatRoomRepository;
import com.example.demo.repository.MessageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository roomRepository;
    private final MessageRepository messageRepository;

    // ── Rooms ─────────────────────────────────────────────────────────────────
    @Transactional
    public ChatRoom createRoom(String name, String description, User creator) {
        ChatRoom room = ChatRoom.builder()
                .name(name).description(description).createdBy(creator).build();
        room.getMembers().add(creator);
        return roomRepository.save(room);
    }

    @Transactional
    public ChatRoom joinRoom(Long roomId, User user) {
        ChatRoom room = findRoom(roomId);
        room.getMembers().add(user);
        return roomRepository.save(room);
    }

    @Transactional
    public void leaveRoom(Long roomId, User user) {
        ChatRoom room = findRoom(roomId);
        room.getMembers().remove(user);
        roomRepository.save(room);
    }

    public ChatRoom findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + id));
    }

    public List<ChatRoom> getAllRooms() {
        return roomRepository.findAllOrderByCreatedAt();
    }

    // ── Messages ──────────────────────────────────────────────────────────────
    @Transactional
    public Message saveMessage(Long roomId, User sender, String content, MessageType type) {
        ChatRoom room = findRoom(roomId);
        return messageRepository.save(Message.builder()
                .content(content).sender(sender).room(room).type(type).build());
    }

    public List<Message> getRoomMessages(Long roomId) {
        return messageRepository.findByRoomIdAsc(roomId);
    }

    // ── Seed ─────────────────────────────────────────────────────────────────
    @Transactional
    public void seedDefaultRooms(User admin) {
        if (roomRepository.count() == 0) {
            createRoom("general",   "General discussion for everyone", admin);
            createRoom("random",    "Off-topic conversations",          admin);
            createRoom("tech-talk", "All things technology",            admin);
        }
    }
}

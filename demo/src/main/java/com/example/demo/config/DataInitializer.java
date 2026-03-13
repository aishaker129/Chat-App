package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.service.chat.ChatService;
import com.example.demo.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;
    private final ChatService chatService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            User admin = userService.register("admin", "admin123", "admin@nexus.chat");
            chatService.seedDefaultRooms(admin);
            log.info("✅  Seeded: admin/admin123 + 3 default rooms");
        } catch (Exception e) {
            log.debug("Seed skipped: {}", e.getMessage());
        }
    }
}

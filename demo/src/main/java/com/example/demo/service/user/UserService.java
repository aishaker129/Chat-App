package com.example.demo.service.user;

import com.example.demo.entity.User;
import com.example.demo.enums.UserStatus;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String[] COLORS = {
            "#FF6B6B","#4ECDC4","#45B7D1","#96CEB4",
            "#FFEAA7","#DDA0DD","#98D8C8","#F7DC6F","#BB8FCE","#85C1E9"
    };

    // ── UserDetailsService ────────────────────────────────────────────────────
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), Collections.emptyList());
    }

    // ── Registration ──────────────────────────────────────────────────────────
    @Transactional
    public User register(String username, String password, String email) {
        if (userRepository.existsByUsername(username))
            throw new IllegalArgumentException("Username already taken");
        if (userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Email already registered");

        String color = COLORS[(int)(Math.random() * COLORS.length)];
        return userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .displayName(username)
                .avatarColor(color)
                .status(UserStatus.ONLINE)
                .build());
    }

    // ── Status ────────────────────────────────────────────────────────────────
    @Transactional
    public void setStatus(String username, UserStatus status) {
        userRepository.findByUsername(username).ifPresent(u -> {
            u.setStatus(status);
            u.setLastSeen(LocalDateTime.now());
            userRepository.save(u);
        });
    }

    // ── Queries ───────────────────────────────────────────────────────────────
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}

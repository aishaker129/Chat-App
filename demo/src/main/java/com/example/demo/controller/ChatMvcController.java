package com.example.demo.controller;

import com.example.demo.entity.ChatRoom;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.service.chat.ChatService;
import com.example.demo.service.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatMvcController {

    private final ChatService chatService;
    private final UserService userService;

    // ── Redirect root ──────────────────────────────────────────────────────────
    @GetMapping("/")
    public String root() {
        return "redirect:/chat";
    }

    // ── Chat home — room list ─────────────────────────────────────────────────
    @GetMapping("/chat")
    public String chatHome(Model model,
                           @AuthenticationPrincipal UserDetails principal) {
        User me    = userService.getByUsername(principal.getUsername());
        List<ChatRoom> rooms = chatService.getAllRooms();

        model.addAttribute("me", me);
        model.addAttribute("rooms", rooms);
        model.addAttribute("newRoomForm", new NewRoomForm());
        return "chat/home";
    }

    // ── Create room ────────────────────────────────────────────────────────────
    @PostMapping("/chat/rooms")
    public String createRoom(@Valid @ModelAttribute("newRoomForm") NewRoomForm form,
                             BindingResult result,
                             @AuthenticationPrincipal UserDetails principal,
                             RedirectAttributes flash,
                             Model model) {
        if (result.hasErrors()) {
            User me = userService.getByUsername(principal.getUsername());
            model.addAttribute("me", me);
            model.addAttribute("rooms", chatService.getAllRooms());
            return "chat/home";
        }
        User me = userService.getByUsername(principal.getUsername());
        chatService.createRoom(form.getName(), form.getDescription(), me);
        flash.addFlashAttribute("success", "Room '" + form.getName() + "' created!");
        return "redirect:/chat";
    }

    // ── Room page — join + show messages ─────────────────────────────────────
    @GetMapping("/chat/rooms/{roomId}")
    public String roomPage(@PathVariable Long roomId,
                           Model model,
                           @AuthenticationPrincipal UserDetails principal) {
        User          me       = userService.getByUsername(principal.getUsername());
        ChatRoom      room     = chatService.joinRoom(roomId, me);   // auto-join on enter
        List<Message> messages = chatService.getRoomMessages(roomId);
        List<ChatRoom> rooms   = chatService.getAllRooms();
        List<User>    users    = userService.getAllUsers();

        model.addAttribute("me", me);
        model.addAttribute("room", room);
        model.addAttribute("messages", messages);
        model.addAttribute("rooms", rooms);
        model.addAttribute("users", users);
        return "chat/room";
    }

    // ── Leave room ─────────────────────────────────────────────────────────────
    @PostMapping("/chat/rooms/{roomId}/leave")
    public String leaveRoom(@PathVariable Long roomId,
                            @AuthenticationPrincipal UserDetails principal,
                            RedirectAttributes flash) {
        User me = userService.getByUsername(principal.getUsername());
        chatService.leaveRoom(roomId, me);
        flash.addFlashAttribute("info", "You left the room.");
        return "redirect:/chat";
    }

    // ── Members list page ─────────────────────────────────────────────────────
    @GetMapping("/chat/members")
    public String members(Model model,
                          @AuthenticationPrincipal UserDetails principal) {
        model.addAttribute("me", userService.getByUsername(principal.getUsername()));
        model.addAttribute("users", userService.getAllUsers());
        return "chat/members";
    }

    // ── Form DTO ──────────────────────────────────────────────────────────────
    @Data
    public static class NewRoomForm {
        @NotBlank(message = "Room name is required")
        @Size(min = 2, max = 80, message = "Name must be 2–80 characters")
        private String name;

        @Size(max = 255)
        private String description;
    }
}

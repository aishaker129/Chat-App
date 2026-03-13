package com.example.demo.repository;

import com.example.demo.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT r FROM ChatRoom r ORDER BY r.createdAt ASC")
    List<ChatRoom> findAllOrderByCreatedAt();

    @Query("SELECT r FROM ChatRoom r JOIN r.members m WHERE m.id = :userId ORDER BY r.createdAt ASC")
    List<ChatRoom> findByMemberId(Long userId);
}

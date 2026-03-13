package com.example.demo.repository;

import com.example.demo.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m JOIN FETCH m.sender " +
            "WHERE m.room.id = :roomId AND m.deleted = false " +
            "ORDER BY m.sentAt ASC")
    List<Message> findByRoomIdAsc(Long roomId);
}

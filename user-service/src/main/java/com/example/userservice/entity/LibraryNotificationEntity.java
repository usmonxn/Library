package com.example.userservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "library_notifications")
public class LibraryNotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type;
    private Long userId;
    private Long bookId;
    private Long borrowId;
    @Column(length = 1000)
    private String message;
    private Boolean read;
    @CreationTimestamp
    private LocalDateTime createdAt;
}

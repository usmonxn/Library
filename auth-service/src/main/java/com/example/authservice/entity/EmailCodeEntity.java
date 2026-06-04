package com.example.authservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "email_codes")
public class EmailCodeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String gmail;
    private String code;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    private Boolean used;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

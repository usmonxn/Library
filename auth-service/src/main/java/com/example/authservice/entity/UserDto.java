package com.example.authservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String ism;
    private String familya;
    private String jinsi;
    private String gmail;
    private String phoneNumber;
    private String password;
    private Integer birthYear;
    private Boolean isAdmin;
    private LocalDateTime createdAt;
}

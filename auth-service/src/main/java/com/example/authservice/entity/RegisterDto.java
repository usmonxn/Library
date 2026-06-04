package com.example.authservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDto {
    private Long id;
    private String ism;
    private String familya;
    private String jinsi;
    private String gmail;
    private String phoneNumber;
    private String password;
    @JsonProperty("birthYear")
    private Integer birthDate;
    private Boolean isAdmin;
    private LocalDateTime createdAt;
}

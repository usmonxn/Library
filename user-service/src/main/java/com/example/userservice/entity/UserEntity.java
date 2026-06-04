package com.example.userservice.entity;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ism;
    private  String familya;
    private String jinsi;
    @Column(unique = true)
    private String gmail;
    @Column(name = "gmail_code")
    private String gmailCode;
    @Column(name = "phone_number")
    private String phoneNumber;
    private String password;
    @Column(name = "birth_year")
    private Integer birthYear;
    @Column(name = "is_admin")
    private Boolean isAdmin;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @JsonIgnore
    @Column(name = "profile_image_encrypted", columnDefinition = "bytea")
    private byte[] profileImageEncrypted;
    @Column(name = "profile_image_content_type")
    private String profileImageContentType;

    public Boolean getHasProfileImage() {
        return profileImageEncrypted != null && profileImageEncrypted.length > 0;
    }
}

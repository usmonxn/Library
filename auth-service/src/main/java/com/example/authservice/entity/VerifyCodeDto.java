package com.example.authservice.entity;

import lombok.Data;

@Data
public class VerifyCodeDto {
    private String gmail;
    private String code;
}

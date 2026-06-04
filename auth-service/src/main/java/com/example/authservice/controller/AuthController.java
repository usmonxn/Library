package com.example.authservice.controller;

import com.example.authservice.entity.LoginDto;
import com.example.authservice.entity.LoginResponse;
import com.example.authservice.entity.RegisterDto;
import com.example.authservice.entity.SendCodeDto;
import com.example.authservice.entity.VerifyCodeDto;
import com.example.authservice.exception.CustomException;
import com.example.authservice.service.AuthService;
import com.example.authservice.service.EmailCodeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final EmailCodeService emailCodeService;

    @PostMapping(path = "/register")
    public String addUser(@RequestBody RegisterDto registerDto) {
        authService.register(registerDto);
        return "Ma'lumotlar saqlandi. Endi gmailga tasdiqlash kodi yuboriladi";
    }

    @PostMapping(path = "/login")
    public LoginResponse login(@RequestBody LoginDto loginDto) {
        return authService.login(loginDto);
    }

    @PostMapping(path = "/send-code")
    public String sendCode(@RequestBody SendCodeDto sendCodeDto) {
        emailCodeService.sendCode(sendCodeDto.getGmail());
        return "Kod gmailga jonatildi";
    }

    @PostMapping(path = "/verify-code")
    public String verifyCode(@RequestBody VerifyCodeDto verifyCodeDto) {
        emailCodeService.verifyCode(verifyCodeDto.getGmail(), verifyCodeDto.getCode());
        return "Siz muvaffaqiyatli registratsiyadan o'tildi";
    }
}

package com.example.authservice.service;

import com.example.authservice.client.UserClient;
import com.example.authservice.entity.LoginDto;
import com.example.authservice.entity.LoginResponse;
import com.example.authservice.entity.RegisterDto;
import com.example.authservice.entity.UserDto;
import com.example.authservice.exception.CustomException;
import lombok.AllArgsConstructor;
import feign.FeignException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserClient userClient;

    public void register(RegisterDto request) {
        validateRegisterRequest(request);

        try {
            userClient.checkPhone(request.getPhoneNumber());
            userClient.checkGmail(request.getGmail());
            userClient.addUser(request);
        } catch (FeignException exx) {
            throw new CustomException(400, extractFeignMessage(exx));
        }
    }

    private void validateRegisterRequest(RegisterDto request) {
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new CustomException(400, "Telefon raqam kiritilishi kerak");
        }
        if (request.getGmail() == null || request.getGmail().isBlank()) {
            throw new CustomException(400, "Gmail kiritilishi kerak");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new CustomException(400, "Parol kiritilishi kerak");
        }
        if (request.getJinsi() == null || request.getJinsi().isBlank()) {
            throw new CustomException(400, "Jins tanlanishi kerak");
        }
    }

    private String extractFeignMessage(FeignException exx) {
        String response = exx.contentUTF8();
        if (response != null && response.contains("\"description\":\"")) {
            return response.split("\"description\":\"")[1].split("\"")[0];
        }
        if (response != null && response.contains("\"error\":\"")) {
            return response.split("\"error\":\"")[1].split("\"")[0];
        }
        return "Ro'yxatdan o'tishda xato yuz berdi";
    }

    public LoginResponse login(LoginDto loginDto) {
        if (loginDto.getPhoneNumber() == null || loginDto.getPhoneNumber().isBlank()
                || loginDto.getPassword() == null || loginDto.getPassword().isBlank()) {
            throw new CustomException(400, "Telefon raqam va parol kiritilishi kerak");
        }

        UserDto user;
        try {
            user = userClient.getUserByPhoneNumber(loginDto.getPhoneNumber());
        } catch (FeignException e) {
            throw new CustomException(400, "Telefon raqam yoki parol noto'g'ri");
        }

        if (user == null || user.getPassword() == null) {
            throw new CustomException(400, "Telefon raqam yoki parol noto'g'ri");
        }

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new CustomException(400, "Telefon raqam yoki parol noto'g'ri");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.getId(), user.getPhoneNumber());
    }
}

package com.example.authservice.client;

import com.example.authservice.entity.RegisterDto;
import com.example.authservice.entity.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "${user.service.url:http://localhost:8095}")
public interface UserClient {

    @PostMapping("/users/adduser")
    void addUser(@RequestBody RegisterDto registerDto);

    @GetMapping("/users/phone")
    UserDto getUserByPhoneNumber(@RequestParam String phoneNumber);

    @GetMapping("/users/check-phone")
    void checkPhone(@RequestParam("phoneNumber") String phoneNumber);

    @GetMapping("/users/check-gmail")
    void checkGmail(@RequestParam("gmail") String gmail);

    @PutMapping("/users/gmail-code")
    void updateGmailCode(@RequestParam("gmail") String gmail, @RequestParam("code") String code);

    @DeleteMapping("/users/gmail-code")
    void clearGmailCode(@RequestParam("gmail") String gmail);
}

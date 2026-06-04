package com.example.userservice.controller;

import com.example.userservice.entity.BorrowsEntity;
import com.example.userservice.entity.UserEntity;
import com.example.userservice.entity.AuditLogEntity;
import com.example.userservice.entity.LibraryNotificationEntity;
import com.example.userservice.exception.CustomException;
import com.example.userservice.service.UserService;
import com.example.userservice.service.BorrowsService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final BorrowsService borrowsService;

    @GetMapping("/all-users")
    public List<UserEntity> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{userId}")
    public UserEntity getUserById(@PathVariable Long userId, Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        return userService.getUserById(userId)
                .orElseThrow(() -> new CustomException(404, "User topilmadi"));
    }

    @GetMapping("/phone")
    public UserEntity getUserByPhoneNumber(@RequestParam String phoneNumber) {
        return userService.getUserByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new CustomException(404, "Bunaqa user mavjud emas"));
    }

    @GetMapping("/check-phone")
    public void checkPhone(@RequestParam String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new CustomException(400, "Telefon raqam kiritilishi kerak");
        }
        if (userService.existsByPhoneNumber(phoneNumber)) {
            throw new CustomException(400, "Bu nomer oldin qo'shilgan");
        }
    }

    @GetMapping("/check-gmail")
    public void checkGmail(@RequestParam String gmail) {
        if (gmail == null || gmail.isBlank()) {
            throw new CustomException(400, "Gmail kiritilishi kerak");
        }
        if (userService.existsByGmail(gmail)) {
            throw new CustomException(400, "Bu gmail oldin qo'shilgan");
        }
    }

    @PutMapping("/gmail-code")
    public void updateGmailCode(@RequestParam String gmail, @RequestParam String code) {
        if (gmail == null || gmail.isBlank()) {
            throw new CustomException(400, "Gmail kiritilishi kerak");
        }
        if (code == null || code.isBlank()) {
            throw new CustomException(400, "Kod kiritilishi kerak");
        }
        userService.updateGmailCode(gmail, code);
    }

    @DeleteMapping("/gmail-code")
    public void clearGmailCode(@RequestParam String gmail) {
        if (gmail == null || gmail.isBlank()) {
            throw new CustomException(400, "Gmail kiritilishi kerak");
        }
        userService.clearGmailCode(gmail);
    }

    @PostMapping(path = "/adduser")
    public void addUser(@RequestBody UserEntity userEntity) {
        if (userService.existsByPhoneNumber(userEntity.getPhoneNumber())) {
            throw new CustomException(400, "Bu nomer oldin qo'shilgan");
        }
        if (userEntity.getGmail() != null && userService.existsByGmail(userEntity.getGmail())) {
            throw new CustomException(400, "Bu gmail oldin qo'shilgan");
        }
        userService.addUser(userEntity);
    }

    @PostMapping(path = "/addbook")
    public void createBorrow(@RequestBody BorrowsEntity borrowsEntity, Authentication authentication) {
        if (isAdmin(authentication)) {
            if (borrowsEntity.getUserId() == null) {
                throw new CustomException(403, "User id is required");
            }
        } else {
            borrowsEntity.setUserId(currentUserId(authentication));
        }
        if (borrowsEntity.getUserId() == null) {
            throw new CustomException(403, "User id is required");
        }
        if (borrowsEntity.getBookId() == null) {
            throw new CustomException(403, "Book id is required");
        }
        borrowsService.createBorrow(borrowsEntity);
    }

    @GetMapping(path = "/mybook")
    public List<BorrowsEntity> getMyBooks(@RequestParam Long userId, Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        return borrowsService.getMyBooks(userId);
    }

    @GetMapping(path = "/borrows")
    public List<BorrowsEntity> getAllBorrows() {
        return borrowsService.getAllBorrows();
    }

    @GetMapping(path = "/return-review")
    public List<BorrowsEntity> getReturnReviewBorrows() {
        return borrowsService.getReturnReviewBorrows();
    }

    @PutMapping(path = "/returnbook/{borrowId}")
    public void returnBorrow(@PathVariable Long borrowId, Authentication authentication) {
        borrowsService.returnBorrow(borrowId, isAdmin(authentication) ? null : currentUserId(authentication));
    }

    @PutMapping(path = "/return-review/{borrowId}/confirm")
    public void confirmReturnedNoFine(@PathVariable Long borrowId) {
        borrowsService.confirmReturnedNoFine(borrowId);
    }

    @PutMapping(path = "/return-review/{borrowId}/fine")
    public void sendToFine(@PathVariable Long borrowId) {
        borrowsService.sendToFine(borrowId);
    }

    @GetMapping(path = "/notifications")
    public List<LibraryNotificationEntity> getNotifications() {
        return borrowsService.getNotifications();
    }

    @GetMapping(path = "/audit-logs")
    public List<AuditLogEntity> getAuditLogs() {
        return borrowsService.getAuditLogs();
    }

    @GetMapping(path = "/analytics")
    public Map<String, Object> getAnalytics() {
        return borrowsService.getAnalytics();
    }

    @PutMapping(path = "/{userId}/profile")
    public UserEntity updateProfile(@PathVariable Long userId, @RequestBody UpdateProfileRequest request, Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        return userService.updateName(userId, request.ism(), request.familya());
    }

    @PutMapping(path = "/{userId}/password")
    public void changePassword(@PathVariable Long userId, @RequestBody ChangePasswordRequest request, Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        userService.changePassword(userId, request.currentPassword(), request.newPassword());
    }

    @GetMapping(path = "/{userId}/stats")
    public Map<String, Object> getUserStats(@PathVariable Long userId, Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        return userService.getBorrowStats(userId);
    }

    @PostMapping(path = "/{userId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadProfileImage(@PathVariable Long userId, @RequestParam("file") MultipartFile file, Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        userService.saveProfileImage(userId, file);
    }

    @GetMapping(path = "/{userId}/profile-image")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable Long userId, Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        UserService.ProfileImage image = userService.getProfileImage(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.bytes());
    }

    @DeleteMapping(path = "/{userId}/profile-image")
    public void deleteProfileImage(@PathVariable Long userId, Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        userService.deleteProfileImage(userId);
    }

    private void requireSelfOrAdmin(Long userId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return;
        }
        Long currentUserId = currentUserId(authentication);
        if (currentUserId == null || !currentUserId.equals(userId)) {
            throw new CustomException(403, "Faqat o'zingizga tegishli ma'lumotni ko'rishingiz mumkin");
        }
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomException(401, "Token topilmadi");
        }
        try {
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new CustomException(401, "Token ichida user id topilmadi");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public record UpdateProfileRequest(String ism, String familya) {
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {
    }
}

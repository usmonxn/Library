package com.example.userservice.service;

import com.example.userservice.entity.BorrowsEntity;
import com.example.userservice.entity.UserEntity;
import com.example.userservice.exception.CustomException;
import com.example.userservice.repository.BorrowsRepository;
import com.example.userservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BorrowsRepository borrowsRepository;
    private final ImageCryptoService imageCryptoService;

    public Optional<UserEntity> getUserById(Long id) {
        return userRepository.findById(id);
    }
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }
    public Optional<UserEntity> getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public void addUser(UserEntity userEntity) {
        if (userEntity.getIsAdmin() == null) {
            userEntity.setIsAdmin(false);
        }
        if (userEntity.getBirthYear() == null) {
            userEntity.setBirthYear(2000);
        }
        if (userEntity.getJinsi() == null || userEntity.getJinsi().isBlank()) {
            userEntity.setJinsi("Noma'lum");
        }
        userEntity.setCreatedAt(LocalDateTime.now());
        userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
        userRepository.save(userEntity);
    }


    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    public boolean existsByGmail(String gmail) {
        return userRepository.existsByGmail(gmail);
    }

    public void updateGmailCode(String gmail, String code) {
        UserEntity user = userRepository.findByGmail(gmail)
                .orElseThrow(() -> new CustomException(404, "Bu gmail bilan user topilmadi"));
        user.setGmailCode(code);
        userRepository.save(user);
    }

    public void clearGmailCode(String gmail) {
        userRepository.findByGmail(gmail).ifPresent(user -> {
            user.setGmailCode(null);
            userRepository.save(user);
        });
    }

    public UserEntity updateName(Long userId, String ism, String familya) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "User topilmadi"));
        if (ism == null || ism.isBlank()) {
            throw new CustomException(400, "Ism kiritilishi kerak");
        }
        if (familya == null || familya.isBlank()) {
            throw new CustomException(400, "Familya kiritilishi kerak");
        }
        user.setIsm(ism.trim());
        user.setFamilya(familya.trim());
        return userRepository.save(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "User topilmadi"));
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new CustomException(400, "Joriy parol kiritilishi kerak");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new CustomException(400, "Yangi parol kamida 6 ta belgidan iborat bo'lishi kerak");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new CustomException(400, "Joriy parol noto'g'ri");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void saveProfileImage(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(400, "Profil rasmi tanlanishi kerak");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(400, "Faqat rasm fayl yuklash mumkin");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "User topilmadi"));
        try {
            user.setProfileImageEncrypted(imageCryptoService.encrypt(file.getBytes()));
            user.setProfileImageContentType(contentType);
            userRepository.save(user);
        } catch (Exception e) {
            throw new CustomException(500, "Profil rasmi saqlanmadi");
        }
    }

    public ProfileImage getProfileImage(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "User topilmadi"));
        if (user.getProfileImageEncrypted() == null || user.getProfileImageEncrypted().length == 0) {
            throw new CustomException(404, "Profil rasmi topilmadi");
        }
        return new ProfileImage(
                imageCryptoService.decrypt(user.getProfileImageEncrypted()),
                user.getProfileImageContentType() == null ? "image/jpeg" : user.getProfileImageContentType()
        );
    }

    public void deleteProfileImage(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "User topilmadi"));
        user.setProfileImageEncrypted(null);
        user.setProfileImageContentType(null);
        userRepository.save(user);
    }

    public Map<String, Object> getBorrowStats(Long userId) {
        List<BorrowsEntity> borrows = borrowsRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long active = borrows.stream().filter(b -> "BORROWED".equals(b.getStatus())
                || "RETURN_REVIEW".equals(b.getStatus())
                || "FINE_PENDING".equals(b.getStatus())).count();
        long returned = borrows.stream().filter(b -> "RETURNED".equals(b.getStatus())).count();
        long finePending = borrows.stream().filter(b -> "FINE_PENDING".equals(b.getStatus())).count();
        int paid = borrows.stream().mapToInt(b -> b.getPaidSum() == null ? 0 : b.getPaidSum()).sum();
        int fine = borrows.stream().mapToInt(b -> b.getFineSum() == null ? 0 : b.getFineSum()).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRentals", borrows.size());
        result.put("activeRentals", active);
        result.put("returnedRentals", returned);
        result.put("finePendingRentals", finePending);
        result.put("totalPaid", paid);
        result.put("totalFine", fine);
        return result;
    }

    public record ProfileImage(byte[] bytes, String contentType) {
    }
}

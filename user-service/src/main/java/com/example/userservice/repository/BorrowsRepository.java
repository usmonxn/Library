package com.example.userservice.repository;

import com.example.userservice.entity.BorrowsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowsRepository extends JpaRepository<BorrowsEntity, Long> {
    List<BorrowsEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, String status);
    long countByUserIdAndStatus(Long userId, String status);
    List<BorrowsEntity> findByStatus(String status);
    List<BorrowsEntity> findByStatusOrderByCreatedAtDesc(String status);
}

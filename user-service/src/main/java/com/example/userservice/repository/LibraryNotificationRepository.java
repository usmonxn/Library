package com.example.userservice.repository;

import com.example.userservice.entity.LibraryNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LibraryNotificationRepository extends JpaRepository<LibraryNotificationEntity, Long> {
    List<LibraryNotificationEntity> findTop20ByOrderByCreatedAtDesc();
    boolean existsByTypeAndBorrowId(String type, Long borrowId);
}

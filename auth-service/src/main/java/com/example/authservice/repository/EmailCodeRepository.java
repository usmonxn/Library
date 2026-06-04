package com.example.authservice.repository;

import com.example.authservice.entity.EmailCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailCodeRepository extends JpaRepository<EmailCodeEntity, Long> {
    Optional<EmailCodeEntity> findTopByGmailAndUsedFalseOrderByCreatedAtDesc(String gmail);
}

package com.example.userservice.repository;

import com.example.userservice.entity.BorrowsEntity;
import com.example.userservice.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @Query(nativeQuery = true,value = """
SELECT * FROM users WHERE phone_number = :phoneNumber
""")
    Optional<UserEntity> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByGmail(String gmail);

    Optional<UserEntity> findByGmail(String gmail);

    @Query(nativeQuery = true,value = """
SELECT * FROM borrows WHERE user_id = :userId
""")
    List<BorrowsEntity> getMyBooks(@Param("userId") String userId);
}

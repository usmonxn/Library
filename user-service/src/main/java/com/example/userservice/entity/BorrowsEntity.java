package com.example.userservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "borrows")
public class BorrowsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long bookId;
    @Column(name = "borrow_date")
    private String borrowDate;
    @Column(name = "return_date")
    private String returnDate;
    @Column(name = "due_date")
    private String dueDate;
    @Column(name = "rental_plan")
    private String rentalPlan;
    @Column(name = "rental_days")
    private Integer rentalDays;
    @Column(name = "price_sum")
    private Integer priceSum;
    @Column(name = "overdue_days")
    private Integer overdueDays;
    @Column(name = "fine_sum")
    private Integer fineSum;
    @Column(name = "payment_status")
    private String paymentStatus;
    @Column(name = "paid_sum")
    private Integer paidSum;
    @Column(name = "card_last_four")
    private String cardLastFour;
    @Column(name = "card_holder")
    private String cardHolder;
    @Column(name = "card_expiry")
    private String cardExpiry;
    private String status;
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
}

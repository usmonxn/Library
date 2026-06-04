package com.example.excelbot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Setter
@Getter
@Entity
@Table(name = "products")
public class ProductEntity {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private long skladPrice;

    @Column(nullable = false)
    private double uzumPercent;

    @Column(nullable = false)
    private double uzumCommission;

    @Column(nullable = false)
    private long logistika;

    @Column(nullable = false)
    private long kgt;

    @Column(nullable = false)
    private long sellPrice;

    @Setter
    @Column(nullable = false)
    private double profit;
}

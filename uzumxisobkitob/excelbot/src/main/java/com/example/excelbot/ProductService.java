package com.example.excelbot;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductEntity saveProduct(ProductData data) {
        ProductEntity product = new ProductEntity();
        product.setCreatedAt(LocalDateTime.now());
        product.setProductName(data.getProductName());
        product.setSkladPrice(data.getSkladPrice());
        product.setUzumPercent(data.getUzumPercent());
        product.setUzumCommission(data.getUzumCommission());
        product.setLogistika(data.getLogistika());
        product.setKgt(data.getKgt());
        product.setSellPrice(data.getSellPrice());
        product.setProfit(data.getProfit());

        return productRepository.save(product);
    }
}

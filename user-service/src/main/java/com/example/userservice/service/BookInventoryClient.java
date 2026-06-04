package com.example.userservice.service;

import com.example.userservice.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class BookInventoryClient {
    private final RestClient restClient;
    private final String internalToken;

    public BookInventoryClient(@Value("${book.service.url:http://localhost:8099}") String bookServiceUrl,
                               @Value("${app.internal-token}") String internalToken) {
        this.restClient = RestClient.create(bookServiceUrl);
        this.internalToken = internalToken;
    }

    public void borrowCopy(Long bookId) {
        call("/book/internal/" + bookId + "/borrow", "Kitob nusxasini band qilib bo'lmadi");
    }

    public void returnCopy(Long bookId) {
        call("/book/internal/" + bookId + "/return", "Kitob nusxasini qaytarib bo'lmadi");
    }

    private void call(String path, String message) {
        try {
            restClient.post()
                    .uri(path)
                    .header("X-Internal-Token", internalToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new CustomException(400, message);
        }
    }
}

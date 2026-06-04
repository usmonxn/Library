package com.example.userservice.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomException extends RuntimeException {

    private final int status;
    private final String description;

    public CustomException(int status, String description) {
        super(description);
        this.status = status;
        this.description = description;

    }
}

package com.example.bookservice.exception;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AccessDeniedException extends RuntimeException {
    private final int status;
    private final String description;

    public AccessDeniedException(int status, String description) {
        super(description);
        this.status = status;
        this.description = description;
    }

}

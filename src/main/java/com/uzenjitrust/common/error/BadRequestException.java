package com.uzenjitrust.common.error;

public class BadRequestException extends AppException {

    public BadRequestException(String message) {
        super(400, message);
    }
}

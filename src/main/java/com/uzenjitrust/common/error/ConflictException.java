package com.uzenjitrust.common.error;

public class ConflictException extends AppException {

    public ConflictException(String message) {
        super(409, message);
    }
}

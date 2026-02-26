package com.uzenjitrust.common.error;

public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(404, message);
    }
}

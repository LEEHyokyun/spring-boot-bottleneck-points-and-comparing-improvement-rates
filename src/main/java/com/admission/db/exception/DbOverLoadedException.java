package com.admission.db.exception;

public class DbOverLoadedException extends RuntimeException {
    public DbOverLoadedException(String message) {
        super(message);
    }
}

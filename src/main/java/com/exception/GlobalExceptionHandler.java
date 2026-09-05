package com.exception;

import com.admission.db.exception.DbOverLoadedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DbOverLoadedException.class)
    public ResponseEntity<String> handleDbAdmissionException(
            DbOverLoadedException e) {

        log.error("[GlobalExceptionHandler.DbOverLoadedException] EXCEPTION OCCURED : " + e.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("DB capacity exceeded");
    }

}

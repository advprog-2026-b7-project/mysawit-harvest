package id.ac.ui.cs.advprog.mysawit.harvest.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestErrorResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestApiException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestStorageException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestValidationException;

@RestControllerAdvice
public class HarvestExceptionHandler {

    @ExceptionHandler(HarvestApiException.class)
    public ResponseEntity<HarvestErrorResponse> handleHarvestApiException(HarvestApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new HarvestErrorResponse(
                        "error",
                        ex.getErrorKey(),
                        ex.getMessage(),
                        Instant.now()));
    }

    @ExceptionHandler({BindException.class, HarvestValidationException.class,
            MissingServletRequestPartException.class, MissingRequestHeaderException.class})
    public ResponseEntity<HarvestErrorResponse> handleValidation(Exception ex) {
        HarvestErrorKey errorKey = HarvestErrorKey.VALIDATION_FAILED;
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (ex instanceof MissingServletRequestPartException missingPart
                && "photos".equals(missingPart.getRequestPartName())) {
            errorKey = HarvestErrorKey.NO_PHOTOS_PROVIDED;
            message = "At least one photo is required";
        }

        if (ex instanceof MissingRequestHeaderException missingHeader
                && "Authorization".equalsIgnoreCase(missingHeader.getHeaderName())) {
            errorKey = HarvestErrorKey.FORBIDDEN;
            message = "Authorization header is required";
            status = HttpStatus.FORBIDDEN;
        }

        if (ex instanceof BindException bindException && bindException.getBindingResult().hasErrors()) {
            message = bindException.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }

        return ResponseEntity.status(status).body(new HarvestErrorResponse(
                "error",
                errorKey,
                message,
                Instant.now()));
    }

    @ExceptionHandler(HarvestStorageException.class)
    public ResponseEntity<HarvestErrorResponse> handleStorage(HarvestStorageException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new HarvestErrorResponse(
                        "error",
                        ex.getErrorKey(),
                        ex.getMessage(),
                        Instant.now()));
    }
}
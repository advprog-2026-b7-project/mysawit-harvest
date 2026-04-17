package id.ac.ui.cs.advprog.mysawit.harvest.exception;

import org.springframework.http.HttpStatus;

import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;

public class HarvestApiException extends RuntimeException {

    private final HarvestErrorKey errorKey;
    private final HttpStatus status;

    public HarvestApiException(HarvestErrorKey errorKey, HttpStatus status, String message) {
        super(message);
        this.errorKey = errorKey;
        this.status = status;
    }

    public HarvestErrorKey getErrorKey() {
        return errorKey;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
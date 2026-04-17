package id.ac.ui.cs.advprog.mysawit.harvest.exception;

import org.springframework.http.HttpStatus;

import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;

public class HarvestValidationException extends HarvestApiException {

    public HarvestValidationException(HarvestErrorKey errorKey, String message) {
        super(errorKey, HttpStatus.BAD_REQUEST, message);
    }

    public HarvestValidationException(String message) {
        this(HarvestErrorKey.VALIDATION_FAILED, message);
    }
}
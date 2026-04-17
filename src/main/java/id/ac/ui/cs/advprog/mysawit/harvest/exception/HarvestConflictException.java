package id.ac.ui.cs.advprog.mysawit.harvest.exception;

import org.springframework.http.HttpStatus;

import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;

public class HarvestConflictException extends HarvestApiException {

    public HarvestConflictException(HarvestErrorKey errorKey, String message) {
        super(errorKey, HttpStatus.CONFLICT, message);
    }
}
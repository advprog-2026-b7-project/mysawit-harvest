package id.ac.ui.cs.advprog.mysawit.harvest.exception;

import org.springframework.http.HttpStatus;

import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;

public class HarvestNotFoundException extends HarvestApiException {

    public HarvestNotFoundException(HarvestErrorKey errorKey, String message) {
        super(errorKey, HttpStatus.NOT_FOUND, message);
    }
}
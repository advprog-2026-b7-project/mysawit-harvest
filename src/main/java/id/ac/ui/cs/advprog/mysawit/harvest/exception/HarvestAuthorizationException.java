package id.ac.ui.cs.advprog.mysawit.harvest.exception;

import org.springframework.http.HttpStatus;

import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;

public class HarvestAuthorizationException extends HarvestApiException {

    public HarvestAuthorizationException(HarvestErrorKey errorKey, String message) {
        super(errorKey, HttpStatus.FORBIDDEN, message);
    }

    public HarvestAuthorizationException(String message) {
        this(HarvestErrorKey.FORBIDDEN, message);
    }
}
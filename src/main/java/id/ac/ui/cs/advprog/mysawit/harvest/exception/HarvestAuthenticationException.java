package id.ac.ui.cs.advprog.mysawit.harvest.exception;

import org.springframework.http.HttpStatus;

import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;

public class HarvestAuthenticationException extends HarvestApiException {

    public HarvestAuthenticationException(String message) {
        super(HarvestErrorKey.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, message);
    }
}

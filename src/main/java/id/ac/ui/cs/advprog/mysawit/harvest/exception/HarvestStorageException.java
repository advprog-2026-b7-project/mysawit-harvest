package id.ac.ui.cs.advprog.mysawit.harvest.exception;

import org.springframework.http.HttpStatus;

import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;

public class HarvestStorageException extends HarvestApiException {

    public HarvestStorageException(String message, Throwable cause) {
        super(HarvestErrorKey.STORAGE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, message);
        initCause(cause);
    }
}
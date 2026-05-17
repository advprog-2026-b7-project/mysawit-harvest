package id.ac.ui.cs.advprog.mysawit.harvest.dto;

import java.time.Instant;

import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;

public class HarvestErrorResponse {

    private String status;
    private HarvestErrorKey errorKey;
    private String message;
    private Instant timestamp;

    public HarvestErrorResponse(String status,
            HarvestErrorKey errorKey,
            String message,
            Instant timestamp) {
        this.status = status;
        this.errorKey = errorKey;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public HarvestErrorKey getErrorKey() {
        return errorKey;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

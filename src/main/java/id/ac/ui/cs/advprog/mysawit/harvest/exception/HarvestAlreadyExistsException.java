package id.ac.ui.cs.advprog.mysawit.harvest.exception;

import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;

public class HarvestAlreadyExistsException extends HarvestConflictException {

    public HarvestAlreadyExistsException(String message) {
        super(HarvestErrorKey.HARVEST_ALREADY_SUBMITTED_TODAY, message);
    }
}
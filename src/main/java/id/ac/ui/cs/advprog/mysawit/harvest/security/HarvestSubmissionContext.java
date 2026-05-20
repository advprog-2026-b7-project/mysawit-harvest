package id.ac.ui.cs.advprog.mysawit.harvest.security;

public record HarvestSubmissionContext(
        String buruhId,
        String buruhName,
        String plantationId,
        String role,
        String authorizationHeader) {

    public HarvestSubmissionContext(
            String buruhId,
            String buruhName,
            String plantationId,
            String role) {
        this(buruhId, buruhName, plantationId, role, null);
    }
}

package id.ac.ui.cs.advprog.mysawit.harvest.security;

public record HarvestReviewerContext(
        String userId,
        String role,
        String name,
        String authorizationHeader) {

    public HarvestReviewerContext(String userId, String role, String name) {
        this(userId, role, name, null);
    }
}

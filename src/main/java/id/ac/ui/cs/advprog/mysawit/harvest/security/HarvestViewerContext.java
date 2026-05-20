package id.ac.ui.cs.advprog.mysawit.harvest.security;

public record HarvestViewerContext(String userId, String role, String authorizationHeader) {

    public HarvestViewerContext(String userId, String role) {
        this(userId, role, null);
    }
}

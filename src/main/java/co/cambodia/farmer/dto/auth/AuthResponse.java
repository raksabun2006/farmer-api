package co.cambodia.farmer.dto.auth;

public record AuthResponse(String token, String tokenType, String username, String role, Long id) {

    public AuthResponse(String token, String username, String role, Long id) {
        this(token, "Bearer", username, role, id);
    }
}
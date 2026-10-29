package co.cambodia.farmer.dto.auth;

import java.util.UUID;

public record AuthResponse(String token, String tokenType, String username, String role, UUID id) {

    public AuthResponse(String token, String username, String role, UUID id) {
        this(token, "Bearer", username, role, id);
    }
}

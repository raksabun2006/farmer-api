package co.cambodia.farmer.controller;

import co.cambodia.farmer.config.JwtTokenProvider;
import co.cambodia.farmer.dto.auth.AuthResponse;
import co.cambodia.farmer.dto.auth.LoginRequest;
import co.cambodia.farmer.dto.auth.RegisterRequest;
import co.cambodia.farmer.dto.auth.RegisterResponse;
import co.cambodia.farmer.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor // Lombok automatically builds the constructor for all final fields
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest
    ) {

        return ResponseEntity.ok(
                authService.login(loginRequest)
        );
    }

    @PostMapping(value = "/register")
    public ResponseEntity<RegisterResponse> registerUser(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        authService.register(registerRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new RegisterResponse(
                                true,
                                "User registered successfully"
                        )
                );
    }
}
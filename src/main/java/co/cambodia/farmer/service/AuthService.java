package co.cambodia.farmer.service;


import co.cambodia.farmer.dto.auth.AuthResponse;
import co.cambodia.farmer.dto.auth.LoginRequest;
import co.cambodia.farmer.dto.auth.RegisterRequest;



public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
    void register(RegisterRequest registerRequest);
}
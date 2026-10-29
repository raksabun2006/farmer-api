package co.cambodia.farmer.service.impl;

import co.cambodia.farmer.domain.User;
import co.cambodia.farmer.domain.Farmer;
import co.cambodia.farmer.dto.auth.AuthResponse;
import co.cambodia.farmer.dto.auth.LoginRequest;
import co.cambodia.farmer.dto.auth.RegisterRequest;
import co.cambodia.farmer.mapper.UserMapper;
import co.cambodia.farmer.repository.UserRepository;
import co.cambodia.farmer.repository.FarmerRepository;
import co.cambodia.farmer.service.AuthService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.secret}")
    private String secretKeyHex;

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        User user = userRepository.findByUsername(loginRequest.username())
                .orElseThrow(() -> new UsernameNotFoundException("រកមិនឃើញអ្នកប្រើប្រាស់ឡើយ"));

        try {
            byte[] decodedKey = secretKeyHex.getBytes(StandardCharsets.UTF_8);
            MACSigner signer = new MACSigner(decodedKey);

            long nowInSeconds = System.currentTimeMillis() / 1000;
            long oneDayInSeconds = 86400;

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getUsername())
                    .claim("iat", nowInSeconds)
                    .claim("exp", nowInSeconds + oneDayInSeconds)
                    .claim("roles", new String[]{user.getRole().name()})
                    .claim("username", user.getUsername())
                    .claim("id", user.getId().toString())
                    .build();
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);
            String token = signedJWT.serialize();

            return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getId());

        } catch (Exception e) {
            throw new RuntimeException("មានបញ្ហាក្នុងការបង្កើតតូខិន", e);
        }
    }

    @Override
    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new RuntimeException("Email is already registered");
        }
        if (userRepository.existsByUsername(registerRequest.username())) {
            throw new RuntimeException("Username is already taken");
        }
        User user = userMapper.toEntity(registerRequest);
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        User savedUser = userRepository.save(user);
        if ("FARMER".equals(savedUser.getRole().name())) {
            Farmer farmer = new Farmer();
            farmer.setUser(savedUser);
            farmer.setFullName(registerRequest.fullName());
            farmer.setPhone(registerRequest.phone());
            farmer.setProvince(registerRequest.province());
            farmer.setProfileImage(registerRequest.profileImage());

            farmerRepository.save(farmer);
        }
    }
}

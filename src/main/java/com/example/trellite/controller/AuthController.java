package com.example.trellite.controller;

import com.example.trellite.dto.LoginRequestDTO;
import com.example.trellite.dto.LoginResponseDTO;
import com.example.trellite.dto.UserRegisterDTO;
import com.example.trellite.dto.UserResponseDTO;
import com.example.trellite.dto.UserSummaryDTO;
import com.example.trellite.model.User;
import com.example.trellite.service.AuthService;
import com.example.trellite.service.JwtService;
import com.example.trellite.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        UserResponseDTO savedUser = authService.register(userRegisterDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        // Throws AuthenticationException on bad credentials, which the advice maps to 401.
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginRequest.username(),
                loginRequest.password()
        ));

        User user = authService.requireUser(loginRequest.username());
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();
        return ResponseEntity.ok(new LoginResponseDTO(accessToken, refreshToken));
    }

    /** Who the bearer token belongs to — lets a client compare by id instead of by name. */
    @GetMapping("/me")
    public ResponseEntity<UserSummaryDTO> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(authService.whoAmI(username));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        refreshTokenService.deleteTokensForUser(username);
        return ResponseEntity.ok("Logged out successfully");
    }
}

package com.example.trellite.controller;

import com.example.trellite.dto.LoginResponseDTO;
import com.example.trellite.dto.RefreshTokenRequestDTO;
import com.example.trellite.model.RefreshToken;
import com.example.trellite.service.JwtService;
import com.example.trellite.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public RefreshTokenController(RefreshTokenService refreshTokenService, JwtService jwtService) {
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    /**
     * Request and response shapes are unchanged, but the refresh token is now rotated:
     * the returned refreshToken is a NEW value and the one that was sent is dead. Clients
     * must store what comes back rather than keeping the token they had.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        RefreshToken rotated = refreshTokenService.rotateRefreshToken(request.refreshToken());
        String newAccessToken = jwtService.generateToken(rotated.getUser());

        return ResponseEntity.ok(new LoginResponseDTO(newAccessToken, rotated.getToken()));
    }
}

package com.myafiliados.controller;

import com.myafiliados.dto.Dtos;
import com.myafiliados.model.Afiliado;
import com.myafiliados.service.AfiliadoAuthService;
import com.myafiliados.service.AfiliadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AfiliadoService service;
    private final AfiliadoAuthService authService;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of("ok", true, "service", "myafiliados-api"));
    }

    @PostMapping("/signup")
    public ResponseEntity<Dtos.AfiliadoResponse> signup(@RequestBody Dtos.SignupRequest req) {
        Afiliado a = service.signup(req);
        return ResponseEntity.ok(Dtos.AfiliadoResponse.de(a));
    }

    @PostMapping("/login")
    public ResponseEntity<Dtos.LoginResponse> login(@RequestBody Dtos.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
}

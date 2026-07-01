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
    public ResponseEntity<?> signup(@RequestBody Dtos.SignupRequest req) {
        try {
            Afiliado a = service.signup(req);
            return ResponseEntity.ok(Dtos.AfiliadoResponse.de(a));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            // Retorna JSON com msg legível pro frontend em vez de 403 mudo
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("erro", e.getReason() != null ? e.getReason() : "Erro no cadastro"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("erro", e.getMessage() != null ? e.getMessage() : "Erro interno"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Dtos.LoginRequest req) {
        try {
            return ResponseEntity.ok(authService.login(req));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("erro", e.getReason() != null ? e.getReason() : "Erro no login"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("erro", "Erro interno"));
        }
    }
}

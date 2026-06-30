package com.myafiliados.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * Geração e validação de JWT pro painel de afiliados.
 *
 * Roles:
 *   AFILIADO        - usuário normal (cadastrou e foi aprovado)
 *   ADMIN_AFILIADOS - admin interno (acessa /admin/* do painel)
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiraMs;

    public JwtUtil(@Value("${mydelivery.afiliados.jwt-secret}") String secret,
                   @Value("${mydelivery.afiliados.jwt-expira-horas:12}") int expiraHoras) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET_AFILIADOS não configurado. Gere com: openssl rand -base64 64");
        }
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(secret); }
        catch (Exception e) { bytes = secret.getBytes(StandardCharsets.UTF_8); }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiraMs = expiraHoras * 3600_000L;
    }

    public String gerar(String subject, String role) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiraMs))
                .signWith(key)
                .compact();
    }

    public Claims validar(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String extrairSubject(String token) {
        try { return validar(token).getSubject(); } catch (Exception e) { return null; }
    }
    public String extrairRole(String token) {
        try { return validar(token).get("role", String.class); } catch (Exception e) { return null; }
    }
    public boolean tokenValido(String token) {
        try { validar(token); return true; } catch (Exception e) { return false; }
    }
}

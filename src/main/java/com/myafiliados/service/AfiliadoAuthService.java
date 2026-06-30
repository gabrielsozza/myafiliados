package com.myafiliados.service;

import com.myafiliados.dto.Dtos;
import com.myafiliados.model.Afiliado;
import com.myafiliados.repository.AfiliadoRepository;
import com.myafiliados.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AfiliadoAuthService {

    private final AfiliadoRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${mydelivery.afiliados.admin-email:gpsozza3@gmail.com}")
    private String adminEmail;
    @Value("${mydelivery.afiliados.admin-senha:}")
    private String adminSenha;

    @Transactional
    public Dtos.LoginResponse login(Dtos.LoginRequest req) {
        if (req == null || req.email == null || req.senha == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }
        String emailNorm = req.email.trim().toLowerCase();

        // ── Login do admin (hardcoded via prop pra simplificar V1) ──
        if (adminSenha != null && !adminSenha.isBlank()
                && adminEmail.equalsIgnoreCase(emailNorm)
                && adminSenha.equals(req.senha)) {
            Dtos.LoginResponse r = new Dtos.LoginResponse();
            r.token = jwtUtil.gerar("admin:" + emailNorm, "ADMIN_AFILIADOS");
            r.role = "ADMIN_AFILIADOS";
            return r;
        }

        // ── Login de afiliado ──
        Afiliado a = repo.findByEmailIgnoreCase(emailNorm)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
        if (!passwordEncoder.matches(req.senha, a.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }
        if (a.getStatus() == Afiliado.Status.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Cadastro ainda em análise. Você recebe um email quando aprovado.");
        }
        if (a.getStatus() == Afiliado.Status.BLOQUEADO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Acesso bloqueado. Entre em contato com o administrador.");
        }
        if (a.getStatus() == Afiliado.Status.INATIVO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta inativa.");
        }
        a.setUltimoLoginEm(LocalDateTime.now());
        repo.save(a);

        Dtos.LoginResponse r = new Dtos.LoginResponse();
        r.token = jwtUtil.gerar("afiliado:" + a.getId(), "AFILIADO");
        r.role = "AFILIADO";
        r.afiliado = Dtos.AfiliadoResponse.de(a);
        return r;
    }

    /** Resolve o afiliado a partir do subject do JWT. */
    public Afiliado afiliadoDoSubject(String subject) {
        if (subject == null || !subject.startsWith("afiliado:")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            Long id = Long.parseLong(subject.substring("afiliado:".length()));
            return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}

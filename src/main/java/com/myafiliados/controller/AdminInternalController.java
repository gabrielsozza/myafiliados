package com.myafiliados.controller;

import com.myafiliados.model.Afiliado;
import com.myafiliados.repository.AfiliadoRepository;
import com.myafiliados.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoints server-to-server chamados pelo admin-mydelivery-api.
 *
 * Segurança: header X-Admin-Secret deve bater com a mesma chave usada
 * no webhook (mydelivery.afiliados.webhook-secret) — evita criar mais
 * uma variável de ambiente separada só pra isso.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin-internal")
@RequiredArgsConstructor
public class AdminInternalController {

    private final JwtUtil jwtUtil;
    private final AfiliadoRepository afiliadoRepo;

    @Value("${mydelivery.afiliados.webhook-secret:}")
    private String adminSecret;

    @Value("${mydelivery.afiliados.admin-email:gpsozza3@gmail.com}")
    private String adminEmail;

    /**
     * SSO — gera um JWT admin válido pra ser embutido em URL do frontend.
     * Usado pelo admin-mydelivery-api pra abrir o painel afiliados já logado.
     */
    @PostMapping("/sso-token")
    public ResponseEntity<Map<String, Object>> ssoToken(
            @RequestHeader(value = "X-Admin-Secret", required = false) String secret) {
        if (adminSecret == null || adminSecret.isBlank()) {
            log.warn("[Admin-Internal] secret vazia — rejeitando SSO");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!adminSecret.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        String token = jwtUtil.gerar("admin:" + adminEmail, "ADMIN_AFILIADOS");
        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", "ADMIN_AFILIADOS",
                "email", adminEmail));
    }

    /**
     * Devolve dados básicos do afiliado por código. Chamado pelo mydelivery-api
     * no momento do cadastro do restaurante pra tirar um "snapshot" e salvar
     * junto do restaurante (imutável). Assim o admin sempre vê "quem indicou"
     * mesmo se depois o afiliado for deletado ou o myafiliados-api ficar offline.
     */
    @GetMapping("/afiliado-por-codigo/{codigo}")
    public ResponseEntity<Map<String, Object>> afiliadoPorCodigo(
            @RequestHeader(value = "X-Admin-Secret", required = false) String secret,
            @PathVariable String codigo) {
        if (adminSecret == null || adminSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!adminSecret.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (codigo == null || codigo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "codigo obrigatorio");
        }
        Afiliado a = afiliadoRepo.findByCodigo(codigo.trim()).orElse(null);
        if (a == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", "afiliado nao encontrado"));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", a.getId());
        out.put("codigo", a.getCodigo());
        out.put("nome", a.getNome());
        out.put("email", a.getEmail());
        out.put("comissaoPercentual", a.getComissaoPercentual());
        out.put("status", a.getStatus() == null ? null : a.getStatus().name());
        return ResponseEntity.ok(out);
    }
}

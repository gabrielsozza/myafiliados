package com.myafiliados.controller;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Proxy pra abrir o sistema demo do MyDelivery pro afiliado. Fluxo:
 *
 *  1. Afiliado clica "Abrir demo" em materiais.html.
 *  2. Frontend chama GET /api/afiliado/demo/abrir-sistema (este controller).
 *  3. Este controller chama POST no mydelivery-api /api/afiliado/demo/token
 *     com X-Afiliados-Secret + email do afiliado (auditoria).
 *  4. mydelivery-api devolve {url, expiraEmMinutos}.
 *  5. Devolvemos essa URL pro frontend, que abre em nova aba.
 *
 * Por que proxy em vez de chamar direto do frontend:
 *  - Segredo (X-Afiliados-Secret) precisa ficar no server, nunca no browser.
 *  - Autenticação do afiliado é validada aqui (JWT do painel afiliados) —
 *    afiliado tem que estar logado E aprovado pra abrir a demo.
 */
@Slf4j
@RestController
@RequestMapping("/api/afiliado/demo")
@RequiredArgsConstructor
public class DemoProxyController {

    /** URL base do mydelivery-api. */
    @Value("${mydelivery.api.url:${MYDELIVERY_API_URL:https://api.mydeliveryfood.com.br}}")
    private String mydeliveryApiUrl;

    /** Secret compartilhado com o mydelivery-api. Precisa bater no outro lado. */
    @Value("${afiliados.demo.secret:${AFILIADOS_DEMO_SECRET:}}")
    private String secret;

    @GetMapping("/abrir-sistema")
    @PreAuthorize("hasRole('AFILIADO')")
    public ResponseEntity<Map<String, Object>> abrirSistema(@AuthenticationPrincipal String emailAfiliado) {
        if (secret == null || secret.isBlank()) {
            log.error("DEMO_PROXY: AFILIADOS_DEMO_SECRET não configurado no myafiliados-api");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("erro", "Demo indisponível — configuração pendente"));
        }

        try {
            RestClient http = RestClient.builder()
                    .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                        setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
                        setReadTimeout((int) Duration.ofSeconds(15).toMillis());
                    }})
                    .build();

            String urlDestino = mydeliveryApiUrl.replaceAll("/$", "") + "/api/afiliado/demo/token";
            Map<String, Object> resposta = http.post()
                    .uri(urlDestino)
                    .header("X-Afiliados-Secret", secret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "afiliadoEmail", emailAfiliado == null ? "" : emailAfiliado,
                            "afiliadoId", emailAfiliado == null ? "" : emailAfiliado
                    ))
                    .retrieve()
                    .body(Map.class);

            if (resposta == null || resposta.get("url") == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("erro", "Demo indisponível no momento"));
            }
            log.info("DEMO_PROXY: sessão demo aberta pra afiliado='{}'", emailAfiliado);
            return ResponseEntity.ok(Map.of(
                    "url", resposta.get("url"),
                    "expiraEmMinutos", resposta.getOrDefault("expiraEmMinutos", 30)
            ));
        } catch (Exception e) {
            log.warn("DEMO_PROXY: falha ao chamar mydelivery-api pra abrir demo — {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("erro", "Demo indisponível no momento — tente daqui a pouco"));
        }
    }
}

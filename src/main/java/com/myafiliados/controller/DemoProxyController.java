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
        // Diagnóstico direto: se o proxy nem tá configurado, dizemos O QUE falta.
        // Sem essa mensagem clara, o admin fica adivinhando por que 503 aparece.
        if (secret == null || secret.isBlank()) {
            log.error("DEMO_PROXY: AFILIADOS_DEMO_SECRET não configurado no myafiliados-api");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "erro", "Demo não configurado — falta setar AFILIADOS_DEMO_SECRET no Railway (myafiliados-api)."));
        }
        if (mydeliveryApiUrl == null || mydeliveryApiUrl.isBlank()) {
            log.error("DEMO_PROXY: MYDELIVERY_API_URL não configurado no myafiliados-api");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "erro", "Demo não configurado — falta setar MYDELIVERY_API_URL no Railway (myafiliados-api)."));
        }

        String urlDestino = mydeliveryApiUrl.replaceAll("/$", "") + "/api/afiliado/demo/token";
        try {
            RestClient http = RestClient.builder()
                    .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                        setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
                        setReadTimeout((int) Duration.ofSeconds(15).toMillis());
                    }})
                    .build();

            // Usa toEntity pra pegar body em qualquer status (2xx OU erro) e propagar
            // a mensagem específica do mydelivery-api (secret errado, restaurante
            // demo não existe, etc). Sem isso, qualquer 4xx/5xx vira genérico.
            org.springframework.http.ResponseEntity<Map> ent = http.post()
                    .uri(urlDestino)
                    .header("X-Afiliados-Secret", secret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "afiliadoEmail", emailAfiliado == null ? "" : emailAfiliado,
                            "afiliadoId", emailAfiliado == null ? "" : emailAfiliado
                    ))
                    .retrieve()
                    .onStatus(status -> true, (req, res) -> { /* nao lanca — deixa a gente ler o body */ })
                    .toEntity(Map.class);

            Map<String, Object> resposta = ent.getBody();

            if (ent.getStatusCode().is2xxSuccessful() && resposta != null && resposta.get("url") != null) {
                log.info("DEMO_PROXY: sessão demo aberta pra afiliado='{}'", emailAfiliado);
                return ResponseEntity.ok(Map.of(
                        "url", resposta.get("url"),
                        "expiraEmMinutos", resposta.getOrDefault("expiraEmMinutos", 30)
                ));
            }

            // Extrai mensagem específica se veio (ex: "restaurante demo não configurado")
            String erroBackend = resposta != null && resposta.get("erro") != null
                    ? String.valueOf(resposta.get("erro"))
                    : "resposta sem detalhe (HTTP " + ent.getStatusCode().value() + ")";
            log.warn("DEMO_PROXY: mydelivery-api rejeitou — status={}, erro='{}'", ent.getStatusCode().value(), erroBackend);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "erro", "Demo indisponível: " + erroBackend
            ));
        } catch (Exception e) {
            // Falha de rede/timeout/URL inválida — separada pra distinguir de erro de negócio
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.warn("DEMO_PROXY: falha ao chamar mydelivery-api em {} — {}", urlDestino, msg);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "erro", "Demo indisponível: não consegui falar com o mydelivery-api (" + msg + ")"
            ));
        }
    }
}

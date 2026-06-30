package com.myafiliados.controller;

import com.myafiliados.dto.Dtos;
import com.myafiliados.service.MydeliveryWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Webhook recebido do mydelivery-api quando algo acontece em restaurante
 * vinculado a afiliado.
 *
 * Segurança: header X-Webhook-Secret deve bater com a config
 * mydelivery.afiliados.webhook-secret (compartilhado entre os 2 services).
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/mydelivery")
@RequiredArgsConstructor
public class WebhookInboundController {

    private final MydeliveryWebhookService service;

    @Value("${mydelivery.afiliados.webhook-secret:}")
    private String secretEsperado;

    @PostMapping("/evento")
    public ResponseEntity<Map<String, Object>> evento(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @RequestBody Dtos.WebhookEventoRequest req) {
        if (secretEsperado == null || secretEsperado.isBlank()) {
            log.warn("[Webhook] config secret vazia — rejeitando tudo");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!secretEsperado.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        boolean ok = service.processar(req);
        return ResponseEntity.ok(Map.of("ok", ok));
    }
}

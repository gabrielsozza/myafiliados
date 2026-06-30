package com.myafiliados.service;

import com.myafiliados.dto.Dtos;
import com.myafiliados.model.Afiliado;
import com.myafiliados.model.AfiliadoVinculo;
import com.myafiliados.repository.AfiliadoRepository;
import com.myafiliados.repository.AfiliadoVinculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Recebe e processa eventos enviados pelo mydelivery-api via webhook.
 *
 * Tipos:
 *   RESTAURANTE_CRIADO  - quando alguém cria conta vindo do link
 *   ASSINOU             - quando pagou primeira mensalidade
 *   CANCELOU            - cancelou plano
 *   TRIAL_EXPIROU       - trial acabou sem assinar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MydeliveryWebhookService {

    private final AfiliadoRepository afiliadoRepo;
    private final AfiliadoVinculoRepository vinculoRepo;

    @Transactional
    public boolean processar(Dtos.WebhookEventoRequest e) {
        if (e == null || e.tipo == null || e.restauranteId == null) {
            log.warn("[Webhook] payload inválido");
            return false;
        }
        switch (e.tipo) {
            case "RESTAURANTE_CRIADO": return tratarCriado(e);
            case "ASSINOU":            return tratarAssinou(e);
            case "CANCELOU":           return tratarCancelou(e);
            case "TRIAL_EXPIROU":      return tratarTrialExpirou(e);
            default:
                log.info("[Webhook] tipo desconhecido: {}", e.tipo);
                return true; // ack mesmo assim
        }
    }

    private boolean tratarCriado(Dtos.WebhookEventoRequest e) {
        if (e.codigoAfiliado == null || e.codigoAfiliado.isBlank()) {
            // Restaurante criou conta SEM link de afiliado — ignora
            return true;
        }
        Afiliado a = afiliadoRepo.findByCodigo(e.codigoAfiliado).orElse(null);
        if (a == null) {
            log.warn("[Webhook] codigoAfiliado={} não existe — ignorando", e.codigoAfiliado);
            return true;
        }
        // Idempotência — se já tem vínculo pra esse restaurante, não duplica
        AfiliadoVinculo v = vinculoRepo.findByRestauranteId(e.restauranteId).orElse(null);
        if (v == null) {
            v = new AfiliadoVinculo();
            v.setAfiliadoId(a.getId());
            v.setRestauranteId(e.restauranteId);
            v.setStatusVinculo(AfiliadoVinculo.StatusVinculo.TRIAL);
            v.setTrialIniciadoEm(LocalDateTime.now());
        }
        atualizarSnapshot(v, e);
        if (e.linkOrigem != null) {
            try { v.setLinkOrigem(AfiliadoVinculo.LinkOrigem.valueOf(e.linkOrigem.toUpperCase())); }
            catch (Exception ignored) {}
        }
        vinculoRepo.save(v);
        log.info("[Webhook] RESTAURANTE_CRIADO vinculo={} afiliado={} restaurante={}",
                v.getId(), a.getId(), e.restauranteId);
        return true;
    }

    private boolean tratarAssinou(Dtos.WebhookEventoRequest e) {
        AfiliadoVinculo v = vinculoRepo.findByRestauranteId(e.restauranteId).orElse(null);
        if (v == null) {
            log.info("[Webhook] ASSINOU mas restaurante={} sem vínculo — ignorando", e.restauranteId);
            return true;
        }
        v.setStatusVinculo(AfiliadoVinculo.StatusVinculo.ATIVO);
        v.setAssinouEm(LocalDateTime.now());
        if (e.planoContratado != null) {
            try { v.setPlanoContratado(AfiliadoVinculo.LinkOrigem.valueOf(e.planoContratado.toUpperCase())); }
            catch (Exception ignored) {}
        }
        v.setValorPlano(e.valorPlano);
        // Calcula valorMensalEquivalente se não veio
        if (e.valorMensalEquivalente != null) {
            v.setValorMensalEquivalente(e.valorMensalEquivalente);
        } else if (e.valorPlano != null && v.getPlanoContratado() != null) {
            int meses = switch (v.getPlanoContratado()) {
                case MENSAL -> 1;
                case SEMESTRAL -> 6;
                case ANUAL -> 12;
            };
            v.setValorMensalEquivalente(e.valorPlano.divide(new BigDecimal(meses), 2, RoundingMode.HALF_UP));
        }
        atualizarSnapshot(v, e);
        vinculoRepo.save(v);
        log.info("[Webhook] ASSINOU vinculo={} plano={} valor={} mensal_eq={}",
                v.getId(), v.getPlanoContratado(), v.getValorPlano(), v.getValorMensalEquivalente());
        return true;
    }

    private boolean tratarCancelou(Dtos.WebhookEventoRequest e) {
        AfiliadoVinculo v = vinculoRepo.findByRestauranteId(e.restauranteId).orElse(null);
        if (v == null) return true;
        v.setStatusVinculo(AfiliadoVinculo.StatusVinculo.CANCELADO);
        v.setCanceladoEm(LocalDateTime.now());
        vinculoRepo.save(v);
        log.info("[Webhook] CANCELOU vinculo={}", v.getId());
        return true;
    }

    private boolean tratarTrialExpirou(Dtos.WebhookEventoRequest e) {
        AfiliadoVinculo v = vinculoRepo.findByRestauranteId(e.restauranteId).orElse(null);
        if (v == null) return true;
        if (v.getStatusVinculo() == AfiliadoVinculo.StatusVinculo.TRIAL) {
            v.setStatusVinculo(AfiliadoVinculo.StatusVinculo.TRIAL_EXPIRADO);
            vinculoRepo.save(v);
        }
        return true;
    }

    private void atualizarSnapshot(AfiliadoVinculo v, Dtos.WebhookEventoRequest e) {
        if (e.restauranteNome != null)  v.setRestauranteNome(e.restauranteNome);
        if (e.restauranteSlug != null)  v.setRestauranteSlug(e.restauranteSlug);
        if (e.restauranteEmail != null) v.setRestauranteEmail(e.restauranteEmail);
    }
}

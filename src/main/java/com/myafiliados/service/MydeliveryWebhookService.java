package com.myafiliados.service;

import com.myafiliados.dto.Dtos;
import com.myafiliados.model.Afiliado;
import com.myafiliados.model.AfiliadoEventoLog;
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
import java.util.LinkedHashMap;
import java.util.Map;

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
    private final EventoLogService eventoLog;
    private final AutoComissaoService autoComissaoService;

    @Transactional
    public boolean processar(Dtos.WebhookEventoRequest e) {
        if (e == null || e.tipo == null) {
            log.warn("[Webhook] payload inválido");
            return false;
        }
        // AUTOINDICACAO_BLOQUEADA vem SEM restauranteId (o cadastro nem chegou a ser criado)
        if ("AUTOINDICACAO_BLOQUEADA".equals(e.tipo)) {
            return tratarAutoindicacao(e);
        }
        if (e.restauranteId == null) {
            log.warn("[Webhook] restauranteId ausente pra {} — ignorando", e.tipo);
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

    private boolean tratarAutoindicacao(Dtos.WebhookEventoRequest e) {
        Afiliado a = e.codigoAfiliado != null
                ? afiliadoRepo.findByCodigo(e.codigoAfiliado).orElse(null)
                : null;
        Map<String, Object> det = new LinkedHashMap<>();
        det.put("emailTentativa", e.emailTentativa);
        det.put("telefoneTentativa", e.telefoneTentativa);
        if (e.flags != null) det.putAll(e.flags);
        eventoLog.registrar(AfiliadoEventoLog.Tipo.AUTOINDICACAO_BLOQUEADA,
                eventoLog.novo()
                        .afiliado(a != null ? a.getId() : null)
                        .codigoAfiliado(e.codigoAfiliado)
                        .descricao("Bloqueado no cadastro: " + (e.descricao != null ? e.descricao : "sem detalhes"))
                        .detalhes(det));
        log.warn("[Webhook] AUTOINDICACAO_BLOQUEADA codigo={} descricao={}",
                e.codigoAfiliado, e.descricao);
        return true;
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
        eventoLog.registrar(AfiliadoEventoLog.Tipo.CADASTRO_CONCLUIDO,
                eventoLog.novo()
                        .afiliado(a.getId())
                        .vinculo(v.getId())
                        .restaurante(e.restauranteId)
                        .codigoAfiliado(e.codigoAfiliado)
                        .descricao("Restaurante '" + safe(e.restauranteNome) + "' criou conta"));
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
        eventoLog.registrar(AfiliadoEventoLog.Tipo.ASSINATURA_REALIZADA,
                eventoLog.novo()
                        .afiliado(v.getAfiliadoId())
                        .vinculo(v.getId())
                        .restaurante(e.restauranteId)
                        .descricao("Assinou plano " + v.getPlanoContratado()
                                + " · valor R$ " + v.getValorPlano()));
        // Comissão IMEDIATA — nasce disponível pra pagamento manual do admin.
        // Se restaurante cancelar/estornar depois, admin cancela a comissão.
        try {
            var comissao = autoComissaoService.gerarComissaoAssinatura(v);
            if (comissao != null) {
                eventoLog.registrar(AfiliadoEventoLog.Tipo.COMISSAO_GERADA,
                        eventoLog.novo()
                                .afiliado(v.getAfiliadoId())
                                .vinculo(v.getId())
                                .restaurante(e.restauranteId)
                                .comissao(comissao.getId())
                                .descricao("Comissão gerada automaticamente após assinatura · R$ "
                                        + comissao.getValor()));
            }
        } catch (Exception ex) {
            log.warn("[Webhook] falha ao gerar comissão auto: {}", ex.getMessage());
        }
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
        eventoLog.registrar(AfiliadoEventoLog.Tipo.ASSINATURA_CANCELADA,
                eventoLog.novo()
                        .afiliado(v.getAfiliadoId())
                        .vinculo(v.getId())
                        .restaurante(e.restauranteId)
                        .descricao("Restaurante cancelou plano"));
        log.info("[Webhook] CANCELOU vinculo={}", v.getId());
        return true;
    }

    private boolean tratarTrialExpirou(Dtos.WebhookEventoRequest e) {
        AfiliadoVinculo v = vinculoRepo.findByRestauranteId(e.restauranteId).orElse(null);
        if (v == null) return true;
        if (v.getStatusVinculo() == AfiliadoVinculo.StatusVinculo.TRIAL) {
            v.setStatusVinculo(AfiliadoVinculo.StatusVinculo.TRIAL_EXPIRADO);
            vinculoRepo.save(v);
            eventoLog.registrar(AfiliadoEventoLog.Tipo.TRIAL_EXPIROU,
                    eventoLog.novo()
                            .afiliado(v.getAfiliadoId())
                            .vinculo(v.getId())
                            .restaurante(e.restauranteId)
                            .descricao("Trial expirou sem converter em assinatura"));
        }
        return true;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private void atualizarSnapshot(AfiliadoVinculo v, Dtos.WebhookEventoRequest e) {
        if (e.restauranteNome != null)  v.setRestauranteNome(e.restauranteNome);
        if (e.restauranteSlug != null)  v.setRestauranteSlug(e.restauranteSlug);
        if (e.restauranteEmail != null) v.setRestauranteEmail(e.restauranteEmail);
    }
}

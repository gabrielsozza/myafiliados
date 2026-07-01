package com.myafiliados.dto;

import com.myafiliados.model.Afiliado;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTOs agrupados pra evitar 20 arquivos minúsculos.
 * Cada inner class = 1 DTO.
 */
public class Dtos {

    /** Cadastro público. Cria afiliado com status=PENDENTE. */
    @Data public static class SignupRequest {
        public String nome;
        public String email;
        public String senha;
        public String telefone;
        public String cpf; // opcional
    }

    /** Login. */
    @Data public static class LoginRequest {
        public String email;
        public String senha;
    }

    @Data public static class LoginResponse {
        public String token;
        public String role;
        public AfiliadoResponse afiliado; // null se admin
    }

    /** Resposta padronizada do afiliado. */
    @Data public static class AfiliadoResponse {
        public Long id;
        public String codigo;
        public String nome;
        public String email;
        public String telefone;
        public String status;
        public BigDecimal comissaoPercentual;
        public String recNomeCompleto;
        public String recBanco;
        public String recChavePix;
        public String recTipoChavePix;
        public LocalDateTime criadoEm;
        public LocalDateTime aprovadoEm;
        public LocalDateTime ultimoLoginEm;
        public static AfiliadoResponse de(Afiliado a) {
            AfiliadoResponse r = new AfiliadoResponse();
            r.id = a.getId();
            r.codigo = a.getCodigo();
            r.nome = a.getNome();
            r.email = a.getEmail();
            r.telefone = a.getTelefone();
            r.status = a.getStatus() != null ? a.getStatus().name() : null;
            r.comissaoPercentual = a.getComissaoPercentual();
            r.recNomeCompleto = a.getRecNomeCompleto();
            r.recBanco = a.getRecBanco();
            r.recChavePix = a.getRecChavePix();
            r.recTipoChavePix = a.getRecTipoChavePix() != null ? a.getRecTipoChavePix().name() : null;
            r.criadoEm = a.getCriadoEm();
            r.aprovadoEm = a.getAprovadoEm();
            r.ultimoLoginEm = a.getUltimoLoginEm();
            return r;
        }
    }

    /** Atualizar dados PIX (afiliado mesmo edita). */
    @Data public static class DadosPixRequest {
        public String nomeCompleto;
        public String banco;
        public String chavePix;
        public String tipoChavePix; // CPF/CNPJ/EMAIL/TELEFONE/ALEATORIA
    }

    /** Resumo de KPI do dashboard. */
    @Data public static class DashboardResponse {
        public long indicados;
        public long emTrial;
        public long ativos;
        public long cancelados;
        public long trialExpirado;
        public BigDecimal taxaConversao; // % (0-100)
        public BigDecimal comissoesGeradas; // soma valor (PENDENTE+PAGA)
        public BigDecimal comissoesPagas;
        public BigDecimal comissoesPendentes;
    }

    /** Webhook inbound vindo do mydelivery-api. */
    @Data public static class WebhookEventoRequest {
        public String tipo; // RESTAURANTE_CRIADO / ASSINOU / CANCELOU / TRIAL_EXPIROU / AUTOINDICACAO_BLOQUEADA
        public String codigoAfiliado; // ex "ab12cd34"
        public Long restauranteId;
        public String restauranteNome;
        public String restauranteSlug;
        public String restauranteEmail;
        public String linkOrigem; // MENSAL/SEMESTRAL/ANUAL
        public String planoContratado;
        public BigDecimal valorPlano;
        public BigDecimal valorMensalEquivalente;
        // ── Anti-autoindicação (só usado em AUTOINDICACAO_BLOQUEADA) ──
        public String emailTentativa;
        public String telefoneTentativa;
        public String descricao;
        public java.util.Map<String, Object> flags;
    }

    /** Admin aprova afiliado pendente. */
    @Data public static class AprovarAfiliadoRequest {
        public String adminEmail;
    }

    /** Admin altera percentual. */
    @Data public static class AjustarComissaoRequest {
        public BigDecimal percentual; // 0-100
    }

    /** Admin marca comissão como paga. */
    @Data public static class PagarComissaoRequest {
        public String observacao;
        public String adminEmail;
    }

    @Data public static class CursoModuloRequest {
        public String titulo;
        public String descricao;
        public String videoUrl;
        public Integer ordem;
        public Boolean ativo;
        public Integer duracaoSegundos;
    }
    @Data public static class MaterialApoioRequest {
        public String titulo;
        public String descricao;
        public String arquivoUrl;
        public String tipo;
        public Integer ordem;
        public Boolean ativo;
        public Long tamanhoBytes;
    }
}

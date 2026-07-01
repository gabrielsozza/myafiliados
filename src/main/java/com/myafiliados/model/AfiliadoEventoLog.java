package com.myafiliados.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Timeline de auditoria — TODOS os eventos relevantes do programa de afiliados
 * ficam registrados aqui pra permitir investigação de fraude e disputa.
 *
 * Imutável por definição (sem @UpdateTimestamp, sem métodos de atualização).
 * Uma vez gravado o evento, jamais é alterado.
 */
@Entity
@Table(name = "afiliado_evento_log", indexes = {
    @Index(name = "idx_ael_afiliado", columnList = "afiliado_id"),
    @Index(name = "idx_ael_restaurante", columnList = "restaurante_id"),
    @Index(name = "idx_ael_tipo", columnList = "tipo"),
    @Index(name = "idx_ael_criado", columnList = "criado_em"),
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AfiliadoEventoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Tipo tipo;

    // FKs opcionais — depende do evento
    @Column(name = "afiliado_id")
    private Long afiliadoId;

    @Column(name = "vinculo_id")
    private Long vinculoId;

    @Column(name = "restaurante_id")
    private Long restauranteId;

    @Column(name = "comissao_id")
    private Long comissaoId;

    @Column(name = "ticket_id")
    private Long ticketId;

    /** Código do afiliado (útil pra eventos "sem afiliadoId conhecido" ainda). */
    @Column(name = "codigo_afiliado", length = 16)
    private String codigoAfiliado;

    /** IP de quem gerou (quando aplicável) — auditoria. */
    @Column(length = 60)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * JSON com dados adicionais (ex: campos detectados na autoindicação,
     * valor da comissão, etc). Não usar pra dados críticos — só contexto.
     */
    @Column(columnDefinition = "TEXT")
    private String detalhes;

    /** Mensagem legível pra humano — aparece no painel de auditoria. */
    @Column(length = 500)
    private String descricao;

    /** Usuário admin que gerou (só pra ações manuais). */
    @Column(name = "admin_email", length = 200)
    private String adminEmail;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public enum Tipo {
        // ── Ciclo do restaurante indicado ─────────────────────────
        /** Alguém abriu link com ?afiliado=X (pré-cadastro). */
        CLIQUE_LINK,
        /** Landing detectou o cookie/param no primeiro carregamento. */
        CADASTRO_INICIADO,
        /** Restaurante criado com sucesso vinculado ao afiliado. */
        CADASTRO_CONCLUIDO,
        /** Restaurante ativou plano (webhook ASSINOU). */
        ASSINATURA_REALIZADA,
        /** Restaurante cancelou plano. */
        ASSINATURA_CANCELADA,
        /** Trial expirou sem converter. */
        TRIAL_EXPIROU,

        // ── Ciclo da comissão ─────────────────────────────────────
        COMISSAO_GERADA,
        COMISSAO_MARCADA_PAGA,
        COMISSAO_CANCELADA,
        COMISSAO_APROVADA_ADMIN,
        COMISSAO_REPROVADA_ADMIN,

        // ── Segurança / fraude ────────────────────────────────────
        /** Detectada tentativa de autoindicação — cadastro bloqueado. */
        AUTOINDICACAO_BLOQUEADA,
        /** Suspeita de fraude registrada por outro sinal (múltiplos cadastros mesma origem, etc). */
        SUSPEITA_FRAUDE,
        /** Restaurante já vinculado a outro afiliado tentou revincular. */
        CONFLITO_INDICACAO,

        // ── Intervenções manuais do admin ─────────────────────────
        AFILIADO_APROVADO,
        AFILIADO_BLOQUEADO,
        AFILIADO_DESBLOQUEADO,
        AFILIADO_EXCLUIDO,
        AFILIADO_COMISSAO_AJUSTADA,
        AFILIADO_SENHA_ALTERADA,
        /** Admin trocou o afiliado responsável por um restaurante manualmente. */
        MUDANCA_MANUAL_AFILIADO,

        // ── Tickets / suporte ─────────────────────────────────────
        TICKET_ABERTO,
        TICKET_RESPONDIDO_AFILIADO,
        TICKET_RESPONDIDO_ADMIN,
        TICKET_FECHADO,
    }
}

package com.myafiliados.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vínculo entre um afiliado e um restaurante que chegou via link de afiliado.
 *
 * Criação:
 *  - Restaurante clica em "mydelivery.com.br/?afiliado=ab12cd34" na landing
 *  - Cookie/localStorage marca o `codigo` (TTL ~30 dias)
 *  - Quando ele cria conta no mydelivery, o backend manda webhook pra
 *    myafiliados-api avisando "restaurante X foi criado via afiliado Y"
 *  - Aqui criamos o vínculo com statusVinculo = CRIADO
 *
 * Mudanças de estado vêm via webhook do mydelivery-api:
 *  - TRIAL → cliente em trial gratuito, sem comissão ainda
 *  - ATIVO → cliente assinou plano, comissões começam a ser geradas
 *  - CANCELADO → cancelou assinatura, comissões param (mas histórico fica)
 *
 * Snapshot de dados (restauranteEmail, Nome, Slug) salvo aqui pra não
 * precisar consultar o mydelivery toda vez que renderiza o dashboard.
 */
@Entity
@Table(name = "afiliado_vinculos",
       indexes = {
           @Index(name = "ix_vinculo_afiliado", columnList = "afiliado_id"),
           @Index(name = "ix_vinculo_restaurante", columnList = "restaurante_id"),
           @Index(name = "ix_vinculo_status", columnList = "status_vinculo")
       },
       uniqueConstraints = {
           // 1 restaurante NUNCA pode estar vinculado a 2 afiliados ao mesmo tempo.
           // Se mudar, é via admin manual (deletar e recriar).
           @UniqueConstraint(name = "uk_vinculo_restaurante", columnNames = "restaurante_id")
       })
@Data
public class AfiliadoVinculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK lógica pro afiliado dono do link. */
    @Column(name = "afiliado_id", nullable = false)
    private Long afiliadoId;

    /** ID do restaurante NO BANCO DO MYDELIVERY (outro service). FK lógica. */
    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    // ─── Snapshot pra dashboard sem cross-database join ──────────────────
    @Column(name = "restaurante_nome", length = 200)
    private String restauranteNome;

    @Column(name = "restaurante_slug", length = 80)
    private String restauranteSlug;

    @Column(name = "restaurante_email", length = 160)
    private String restauranteEmail;

    // ─── Tracking de origem ─────────────────────────────────────────────
    /** Qual link foi clicado: MENSAL, SEMESTRAL ou ANUAL. */
    @Enumerated(EnumType.STRING)
    @Column(name = "link_origem", length = 20)
    private LinkOrigem linkOrigem;

    /** Plano que ele EFETIVAMENTE assinou (pode diferir do link clicado).
     *  Null enquanto não assinou. */
    @Enumerated(EnumType.STRING)
    @Column(name = "plano_contratado", length = 20)
    private LinkOrigem planoContratado;

    /** Valor do plano contratado (R$). Snapshot do momento da assinatura. */
    @Column(name = "valor_plano", precision = 10, scale = 2)
    private BigDecimal valorPlano;

    /** Valor da mensalidade equivalente (anual/6 = parcela mensal pra comissão). */
    @Column(name = "valor_mensal_equivalente", precision = 10, scale = 2)
    private BigDecimal valorMensalEquivalente;

    // ─── Status ─────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status_vinculo", nullable = false, length = 20)
    private StatusVinculo statusVinculo = StatusVinculo.CRIADO;

    // ─── Datas ──────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    /** Quando virou TRIAL (cliente criou conta no mydelivery). */
    @Column(name = "trial_iniciado_em")
    private LocalDateTime trialIniciadoEm;

    /** Quando assinou plano de verdade (primeira cobrança). */
    @Column(name = "assinou_em")
    private LocalDateTime assinouEm;

    /** Quando cancelou. */
    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    public enum LinkOrigem { MENSAL, SEMESTRAL, ANUAL }

    public enum StatusVinculo {
        /** Cookie marcou, mas ainda não criou conta no mydelivery. */
        CRIADO,
        /** Criou conta — em trial gratuito. Sem comissão. */
        TRIAL,
        /** Pagou — comissões mensais sendo geradas. */
        ATIVO,
        /** Cancelou assinatura. Comissões param de gerar. Histórico fica. */
        CANCELADO,
        /** Trial expirou sem assinar — perdeu. */
        TRIAL_EXPIRADO
    }
}

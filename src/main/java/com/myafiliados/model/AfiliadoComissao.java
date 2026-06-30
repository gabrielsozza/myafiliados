package com.myafiliados.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Comissão MENSAL gerada pro afiliado a partir de um vínculo ativo.
 *
 * Regra MENSALIZADA (decisão V1):
 *  - MENSAL R$50 + comissão 33,33% → R$16,67/mês enquanto cliente paga.
 *  - SEMESTRAL R$300 → R$50/mês equivalente × 6 → R$16,67/mês × 6 parcelas.
 *  - ANUAL R$550 → R$45,83/mês equivalente × 12 → R$15,28/mês × 12 parcelas.
 *
 * Geração:
 *  - Job mensal no dia X (configurável) varre vínculos ATIVOS
 *  - Pra cada vínculo, calcula comissão proporcional e cria registro
 *    com status PENDENTE.
 *  - Admin marca PAGA depois de fazer o PIX manual.
 *
 * Unique constraint (vinculo + mesReferencia) garante idempotência —
 * mesmo se o job rodar 2x no mês, não duplica.
 */
@Entity
@Table(name = "afiliado_comissoes",
       indexes = {
           @Index(name = "ix_comissao_afiliado", columnList = "afiliado_id"),
           @Index(name = "ix_comissao_vinculo", columnList = "vinculo_id"),
           @Index(name = "ix_comissao_status", columnList = "status"),
           @Index(name = "ix_comissao_mes", columnList = "mes_referencia")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_comissao_vinculo_mes",
                             columnNames = {"vinculo_id", "mes_referencia"})
       })
@Data
public class AfiliadoComissao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id", nullable = false)
    private Long afiliadoId;

    @Column(name = "vinculo_id", nullable = false)
    private Long vinculoId;

    /** Mês de referência da comissão no formato "YYYY-MM" (ex: "2026-07"). */
    @Column(name = "mes_referencia", nullable = false, length = 7)
    private String mesReferencia;

    /** Valor R$ da comissão (já calculado: valorMensalEquivalente × percentual). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    /** Percentual de comissão aplicado (snapshot — afiliado pode mudar depois). */
    @Column(name = "percentual_aplicado", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualAplicado;

    /** Valor da mensalidade equivalente do plano (snapshot). */
    @Column(name = "valor_mensal_equivalente", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMensalEquivalente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDENTE;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    /** Quando admin marcou como PAGA. */
    @Column(name = "pago_em")
    private LocalDateTime pagoEm;

    /** Email do admin que fez o pagamento manual. */
    @Column(name = "pago_por", length = 160)
    private String pagoPor;

    /** Notas livres do admin: comprovante PIX, txid, observação. */
    @Column(length = 500)
    private String observacao;

    public enum Status {
        /** Recém-gerada — aguardando admin pagar. */
        PENDENTE,
        /** Admin marcou como paga (PIX feito). */
        PAGA,
        /** Cancelada manualmente (ex: cliente cancelou no mesmo mês, estorno). */
        CANCELADA
    }

    /** Helper: pega o mês de referência como YearMonth. */
    public YearMonth getYearMonth() {
        return YearMonth.parse(mesReferencia);
    }
}

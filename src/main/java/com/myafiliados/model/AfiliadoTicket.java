package com.myafiliados.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Chamado aberto pelo afiliado. Mensagens ficam em {@link AfiliadoTicketMensagem}.
 *
 * Ciclo: ABERTO → EM_ANALISE → RESPONDIDO → FECHADO
 */
@Entity
@Table(name = "afiliado_ticket", indexes = {
    @Index(name = "idx_ticket_afiliado", columnList = "afiliado_id"),
    @Index(name = "idx_ticket_status", columnList = "status"),
})
@Data
public class AfiliadoTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id", nullable = false)
    private Long afiliadoId;

    /** Snapshot do nome do afiliado no momento — evita JOIN em toda leitura admin. */
    @Column(name = "afiliado_nome", length = 200)
    private String afiliadoNome;

    @Column(name = "afiliado_email", length = 200)
    private String afiliadoEmail;

    @Column(nullable = false, length = 200)
    private String assunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ABERTO;

    /** Última pessoa que escreveu — usado pra saber quem precisa responder. */
    @Enumerated(EnumType.STRING)
    @Column(name = "ultima_resposta_por", length = 10)
    private Autor ultimaRespostaPor;

    /** Não lido no admin? Se ultimaRespostaPor=AFILIADO e naoLidoAdmin=true, badge aparece. */
    @Column(name = "nao_lido_admin", nullable = false)
    private boolean naoLidoAdmin = true;

    /** Não lido pro afiliado? Se ultimaRespostaPor=ADMIN e naoLidoAfiliado=true, badge aparece. */
    @Column(name = "nao_lido_afiliado", nullable = false)
    private boolean naoLidoAfiliado = false;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "fechado_em")
    private LocalDateTime fechadoEm;

    @Column(name = "fechado_por_email", length = 200)
    private String fechadoPorEmail;

    public enum Status { ABERTO, EM_ANALISE, RESPONDIDO, FECHADO }
    public enum Autor { AFILIADO, ADMIN }
}

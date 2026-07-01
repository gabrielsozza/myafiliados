package com.myafiliados.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Uma mensagem dentro de um ticket. Sempre imutável (sem edit). */
@Entity
@Table(name = "afiliado_ticket_mensagem", indexes = {
    @Index(name = "idx_msg_ticket", columnList = "ticket_id"),
})
@Data
public class AfiliadoTicketMensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AfiliadoTicket.Autor autor;

    @Column(name = "autor_nome", length = 200)
    private String autorNome;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}

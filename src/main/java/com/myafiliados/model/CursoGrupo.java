package com.myafiliados.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Módulo (grupo/pasta) que agrupa várias aulas ({@link CursoModulo}).
 *
 * Ex: "Módulo 1 — Fundamentos", "Módulo 2 — Vendas avançadas", etc.
 *
 * Aula sem grupo (grupoId null) fica na seção "Sem categoria" — permite
 * transição suave dos cadastros antigos que não tinham agrupamento.
 */
@Entity
@Table(name = "curso_grupo", indexes = {
    @Index(name = "ix_grupo_ordem", columnList = "ordem")
})
@Data
public class CursoGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false)
    private Integer ordem = 0;

    @Column(nullable = false)
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}

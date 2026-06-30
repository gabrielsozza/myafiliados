package com.myafiliados.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Módulo de vídeo-aula que o admin cadastra pra os afiliados verem
 * dentro do painel ("como configurar a loja do cliente", etc).
 *
 * Vídeo é referenciado por URL (Cloudinary, Youtube unlisted, Vimeo).
 * V1: admin sobe arquivo pelo painel admin (CloudUpload futuro) ou cola URL.
 */
@Entity
@Table(name = "curso_modulos", indexes = {
    @Index(name = "ix_curso_ordem", columnList = "ordem")
})
@Data
public class CursoModulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    /** URL do vídeo (Cloudinary/Youtube/Vimeo). */
    @Column(name = "video_url", nullable = false, length = 600)
    private String videoUrl;

    /** Ordem de exibição. Menor primeiro. */
    @Column(nullable = false)
    private Integer ordem = 0;

    /** Pode ser ocultado sem deletar (preserva continuidade de URL). */
    @Column(nullable = false)
    private Boolean ativo = true;

    /** Duração em segundos (informativo, opcional). */
    @Column(name = "duracao_segundos")
    private Integer duracaoSegundos;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}

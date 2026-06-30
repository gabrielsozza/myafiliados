package com.myafiliados.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Material de apoio que o afiliado baixa/compartilha (PDF de apresentação
 * do MyDelivery, kit de mídia, etc).
 *
 * V1: admin sobe arquivo no Cloudinary e cola URL aqui. Afiliado vê lista
 * + baixa direto da URL.
 */
@Entity
@Table(name = "materiais_apoio", indexes = {
    @Index(name = "ix_material_ordem", columnList = "ordem")
})
@Data
public class MaterialApoio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    /** URL do arquivo (Cloudinary, S3, etc). */
    @Column(name = "arquivo_url", nullable = false, length = 600)
    private String arquivoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo = Tipo.PDF;

    @Column(nullable = false)
    private Integer ordem = 0;

    @Column(nullable = false)
    private Boolean ativo = true;

    /** Tamanho em bytes (informativo, opcional). */
    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public enum Tipo { PDF, IMAGEM, VIDEO, OUTRO }
}

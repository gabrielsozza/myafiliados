package com.myafiliados.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Conta de afiliado.
 *
 * - Cadastro NÃO é aprovado automaticamente — fica PENDENTE até admin aprovar.
 * - Email é a chave de login (unique).
 * - codigo é o slug curto usado na URL do link (ex: ?afiliado=ab12cd34).
 *   Gerado aleatório no cadastro, NÃO muda nunca (links impressos seguem
 *   funcionando mesmo se mudar nome/email).
 * - Comissão padrão vem da config global, mas admin pode customizar por afiliado.
 * - Dados bancários inline aqui pra simplificar V1. Se virar caso de uso
 *   complexo (múltiplas contas, históricos), migra pra tabela própria.
 */
@Entity
@Table(name = "afiliados",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_afiliado_email", columnNames = "email"),
           @UniqueConstraint(name = "uk_afiliado_codigo", columnNames = "codigo")
       })
@Data
public class Afiliado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Slug curto e estável usado no link público (ex: "ab12cd34"). Imutável. */
    @Column(nullable = false, length = 16)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 160)
    private String email;

    /** BCrypt hash. NUNCA armazenar plain text. */
    @Column(name = "senha_hash", nullable = false, length = 200)
    private String senhaHash;

    @Column(length = 20)
    private String telefone;

    /** CPF (somente dígitos). Opcional na V1 — pode virar obrigatório depois. */
    @Column(length = 14)
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDENTE;

    /**
     * Percentual de comissão do afiliado (ex: 33.33).
     * Default ao criar = configuracao global "comissao_percentual_padrao".
     * Admin pode customizar individualmente pelo painel.
     */
    @Column(name = "comissao_percentual", nullable = false, precision = 5, scale = 2)
    private BigDecimal comissaoPercentual = new BigDecimal("33.33");

    // ─── Dados de recebimento (PIX) ─────────────────────────────────────
    // Inline pra V1. Migra pra tabela própria se virar complexo.
    @Column(name = "rec_nome_completo", length = 120)
    private String recNomeCompleto;
    @Column(name = "rec_banco", length = 80)
    private String recBanco;
    @Column(name = "rec_chave_pix", length = 160)
    private String recChavePix;
    @Enumerated(EnumType.STRING)
    @Column(name = "rec_tipo_chave_pix", length = 20)
    private TipoChavePix recTipoChavePix;

    // ─── Auditoria ──────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    /** Quando admin aprovou o cadastro. */
    @Column(name = "aprovado_em")
    private LocalDateTime aprovadoEm;
    @Column(name = "aprovado_por", length = 160)
    private String aprovadoPor; // email do admin

    /** Quando admin bloqueou. */
    @Column(name = "bloqueado_em")
    private LocalDateTime bloqueadoEm;
    @Column(name = "bloqueado_por", length = 160)
    private String bloqueadoPor;
    @Column(name = "motivo_bloqueio", length = 500)
    private String motivoBloqueio;

    @Column(name = "ultimo_login_em")
    private LocalDateTime ultimoLoginEm;

    public enum Status {
        /** Cadastrou mas admin ainda não aprovou. NÃO consegue logar. */
        PENDENTE,
        /** Aprovado e operando. Pode logar. */
        APROVADO,
        /** Admin bloqueou. NÃO consegue logar — perdeu acesso imediato. */
        BLOQUEADO,
        /** Admin marcou como inativo (perdeu interesse, descontinuado). */
        INATIVO
    }

    /** Tipos de chave PIX aceitos pelo BACEN. */
    public enum TipoChavePix {
        CPF, CNPJ, EMAIL, TELEFONE, ALEATORIA
    }
}

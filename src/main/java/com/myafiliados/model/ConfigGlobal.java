package com.myafiliados.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tabela chave-valor pra configurações globais editáveis em runtime
 * (sem precisar de redeploy). Usado pra:
 *  - comissao_percentual_padrao    (ex: "33.33")
 *  - email_aprovacao_template      (HTML do email)
 *  - politica_termos_url           (link pros termos atualizados)
 *  - dia_geracao_comissoes         (ex: "5" = dia 5 de cada mês)
 *
 * Pra valores estruturados, salvar JSON serializado no campo `valor`.
 */
@Entity
@Table(name = "configs")
@Data
public class ConfigGlobal {

    @Id
    @Column(length = 80)
    private String chave;

    @Column(nullable = false, length = 2000)
    private String valor;

    @Column(length = 300)
    private String descricao;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}

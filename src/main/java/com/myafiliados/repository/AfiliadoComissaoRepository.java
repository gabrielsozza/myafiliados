package com.myafiliados.repository;

import com.myafiliados.model.AfiliadoComissao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AfiliadoComissaoRepository extends JpaRepository<AfiliadoComissao, Long> {

    /** Histórico do afiliado — mais recentes primeiro. */
    List<AfiliadoComissao> findByAfiliadoIdOrderByMesReferenciaDesc(Long afiliadoId);

    /** Idempotência do job mensal: garante 1 comissão por (vinculo, mês). */
    Optional<AfiliadoComissao> findByVinculoIdAndMesReferencia(Long vinculoId, String mesReferencia);

    /** Pra KPI "comissões a receber" no dashboard do afiliado. */
    List<AfiliadoComissao> findByAfiliadoIdAndStatus(Long afiliadoId, AfiliadoComissao.Status status);

    /** Admin: listar todas as comissões pendentes pra pagar. */
    List<AfiliadoComissao> findByStatusOrderByMesReferenciaAscAfiliadoIdAsc(AfiliadoComissao.Status status);

    /** Soma de comissões PAGAS pra o afiliado (dashboard). */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(c.valor), 0) FROM AfiliadoComissao c " +
        "WHERE c.afiliadoId = :afiliadoId AND c.status = :status")
    BigDecimal somaPorAfiliadoEStatus(Long afiliadoId, AfiliadoComissao.Status status);
}

package com.myafiliados.repository;

import com.myafiliados.model.AfiliadoVinculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AfiliadoVinculoRepository extends JpaRepository<AfiliadoVinculo, Long> {

    /** Lista todos os restaurantes que esse afiliado trouxe (dashboard). */
    List<AfiliadoVinculo> findByAfiliadoIdOrderByCriadoEmDesc(Long afiliadoId);

    /** Pra webhook: acha o vínculo de um restaurante específico. */
    Optional<AfiliadoVinculo> findByRestauranteId(Long restauranteId);

    /** Pra job de geração de comissão mensal: pega só os vínculos ATIVOS. */
    List<AfiliadoVinculo> findByStatusVinculo(AfiliadoVinculo.StatusVinculo status);

    /** Count rápido pra KPI dashboard. */
    long countByAfiliadoIdAndStatusVinculo(Long afiliadoId, AfiliadoVinculo.StatusVinculo status);
    long countByAfiliadoId(Long afiliadoId);
}

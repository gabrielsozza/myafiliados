package com.myafiliados.repository;

import com.myafiliados.model.AfiliadoEventoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AfiliadoEventoLogRepository extends JpaRepository<AfiliadoEventoLog, Long> {

    List<AfiliadoEventoLog> findByAfiliadoIdOrderByCriadoEmDesc(Long afiliadoId);
    List<AfiliadoEventoLog> findByRestauranteIdOrderByCriadoEmDesc(Long restauranteId);
    List<AfiliadoEventoLog> findByTipoOrderByCriadoEmDesc(AfiliadoEventoLog.Tipo tipo);

    @Query("SELECT e FROM AfiliadoEventoLog e WHERE e.tipo IN :tipos ORDER BY e.criadoEm DESC")
    List<AfiliadoEventoLog> findByTiposOrderByCriadoEmDesc(@Param("tipos") List<AfiliadoEventoLog.Tipo> tipos);

    @Query("SELECT e FROM AfiliadoEventoLog e WHERE e.criadoEm >= :desde ORDER BY e.criadoEm DESC")
    List<AfiliadoEventoLog> findRecentes(@Param("desde") LocalDateTime desde);

    long countByTipo(AfiliadoEventoLog.Tipo tipo);
}

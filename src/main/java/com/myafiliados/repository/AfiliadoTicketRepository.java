package com.myafiliados.repository;

import com.myafiliados.model.AfiliadoTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AfiliadoTicketRepository extends JpaRepository<AfiliadoTicket, Long> {
    List<AfiliadoTicket> findByAfiliadoIdOrderByAtualizadoEmDesc(Long afiliadoId);
    List<AfiliadoTicket> findAllByOrderByAtualizadoEmDesc();
    long countByStatusNotAndNaoLidoAdminTrue(AfiliadoTicket.Status status);
    long countByAfiliadoIdAndNaoLidoAfiliadoTrue(Long afiliadoId);
}

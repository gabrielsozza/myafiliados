package com.myafiliados.repository;

import com.myafiliados.model.AfiliadoTicketMensagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AfiliadoTicketMensagemRepository extends JpaRepository<AfiliadoTicketMensagem, Long> {
    List<AfiliadoTicketMensagem> findByTicketIdOrderByCriadoEmAsc(Long ticketId);
}

package com.myafiliados.controller;

import com.myafiliados.model.*;
import com.myafiliados.repository.*;
import com.myafiliados.service.EventoLogService;
import com.myafiliados.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Endpoints do painel admin — Auditoria + gestão de Tickets.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN_AFILIADOS')")
@RequiredArgsConstructor
public class AdminAuditoriaController {

    private final AfiliadoEventoLogRepository eventoRepo;
    private final AfiliadoTicketRepository ticketRepo;
    private final AfiliadoRepository afiliadoRepo;
    private final AfiliadoVinculoRepository vinculoRepo;
    private final AfiliadoComissaoRepository comissaoRepo;
    private final TicketService ticketService;
    private final EventoLogService eventoLog;

    // ───────────────────────── AUDITORIA ─────────────────────────

    /** Últimos eventos do sistema (auditoria geral). */
    @GetMapping("/auditoria/eventos")
    public ResponseEntity<List<AfiliadoEventoLog>> eventos(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Long afiliadoId,
            @RequestParam(required = false) Long restauranteId) {
        if (afiliadoId != null) {
            return ResponseEntity.ok(eventoRepo.findByAfiliadoIdOrderByCriadoEmDesc(afiliadoId));
        }
        if (restauranteId != null) {
            return ResponseEntity.ok(eventoRepo.findByRestauranteIdOrderByCriadoEmDesc(restauranteId));
        }
        if (tipo != null && !tipo.isBlank()) {
            try {
                var t = AfiliadoEventoLog.Tipo.valueOf(tipo.toUpperCase());
                return ResponseEntity.ok(eventoRepo.findByTipoOrderByCriadoEmDesc(t));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(List.of());
            }
        }
        var recentes = eventoRepo.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "criadoEm"));
        // Limita a 200 pra evitar payload gigante
        if (recentes.size() > 200) recentes = recentes.subList(0, 200);
        return ResponseEntity.ok(recentes);
    }

    /** Suspeitas de autoindicação — casos bloqueados no cadastro. */
    @GetMapping("/auditoria/autoindicacoes")
    public ResponseEntity<List<AfiliadoEventoLog>> autoindicacoes() {
        return ResponseEntity.ok(
                eventoRepo.findByTipoOrderByCriadoEmDesc(
                        AfiliadoEventoLog.Tipo.AUTOINDICACAO_BLOQUEADA));
    }

    /** Resumo geral pro painel — contadores. */
    @GetMapping("/auditoria/resumo")
    public ResponseEntity<Map<String, Object>> resumo() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("autoindicacoesBloqueadas",
                eventoRepo.countByTipo(AfiliadoEventoLog.Tipo.AUTOINDICACAO_BLOQUEADA));
        out.put("suspeitasFraude",
                eventoRepo.countByTipo(AfiliadoEventoLog.Tipo.SUSPEITA_FRAUDE));
        out.put("comissoesPendentes",
                comissaoRepo.findByStatusOrderByMesReferenciaAscAfiliadoIdAsc(
                        AfiliadoComissao.Status.PENDENTE).size());
        out.put("ticketsAbertosNaoLidos",
                ticketRepo.countByStatusNotAndNaoLidoAdminTrue(AfiliadoTicket.Status.FECHADO));
        return ResponseEntity.ok(out);
    }

    /** Timeline consolidada de um restaurante específico. */
    @GetMapping("/auditoria/restaurante/{id}/timeline")
    public ResponseEntity<Map<String, Object>> timelineRestaurante(@PathVariable Long id) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("eventos", eventoRepo.findByRestauranteIdOrderByCriadoEmDesc(id));
        vinculoRepo.findByRestauranteId(id).ifPresent(v -> {
            out.put("vinculo", v);
            afiliadoRepo.findById(v.getAfiliadoId()).ifPresent(a -> out.put("afiliado", a));
        });
        return ResponseEntity.ok(out);
    }

    // ───────────────────────── TICKETS ─────────────────────────

    @GetMapping("/tickets")
    public ResponseEntity<List<AfiliadoTicket>> listarTickets() {
        return ResponseEntity.ok(ticketService.listarTodos());
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<Map<String, Object>> detalheTicket(@PathVariable Long id) {
        AfiliadoTicket t = ticketRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var mensagens = ticketService.mensagensDo(id);
        ticketService.marcarComoLido(id, AfiliadoTicket.Autor.ADMIN);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ticket", t);
        out.put("mensagens", mensagens);
        return ResponseEntity.ok(out);
    }

    @PostMapping("/tickets/{id}/mensagens")
    public ResponseEntity<AfiliadoTicketMensagem> responderTicket(
            @AuthenticationPrincipal String subject,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String adminEmail = extrairEmailAdmin(subject);
        return ResponseEntity.ok(
                ticketService.responderAdmin(adminEmail, id, body.get("mensagem")));
    }

    @PostMapping("/tickets/{id}/fechar")
    public ResponseEntity<AfiliadoTicket> fecharTicket(@AuthenticationPrincipal String subject,
                                                       @PathVariable Long id) {
        String adminEmail = extrairEmailAdmin(subject);
        return ResponseEntity.ok(
                ticketService.fechar(id, adminEmail, AfiliadoTicket.Autor.ADMIN));
    }

    private String extrairEmailAdmin(String subject) {
        if (subject == null) return "admin";
        return subject.startsWith("admin:") ? subject.substring(6) : subject;
    }
}

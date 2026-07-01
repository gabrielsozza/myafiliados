package com.myafiliados.controller;

import com.myafiliados.model.Afiliado;
import com.myafiliados.model.AfiliadoTicket;
import com.myafiliados.model.AfiliadoTicketMensagem;
import com.myafiliados.service.AfiliadoAuthService;
import com.myafiliados.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chamados (tickets) do afiliado pro admin.
 * Todos os endpoints exigem role AFILIADO.
 */
@RestController
@RequestMapping("/api/afiliado/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AFILIADO')")
public class TicketController {

    private final TicketService ticketService;
    private final AfiliadoAuthService authService;

    @GetMapping
    public ResponseEntity<List<AfiliadoTicket>> listar(@AuthenticationPrincipal String subject) {
        Afiliado a = authService.afiliadoDoSubject(subject);
        return ResponseEntity.ok(ticketService.listarDoAfiliado(a));
    }

    @PostMapping
    public ResponseEntity<AfiliadoTicket> abrir(@AuthenticationPrincipal String subject,
                                                @RequestBody Map<String, String> body) {
        Afiliado a = authService.afiliadoDoSubject(subject);
        AfiliadoTicket t = ticketService.abrir(a,
                body.get("assunto"), body.get("mensagem"));
        return ResponseEntity.ok(t);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detalhe(@AuthenticationPrincipal String subject,
                                                        @PathVariable Long id) {
        Afiliado a = authService.afiliadoDoSubject(subject);
        var mensagens = ticketService.mensagensDo(id);
        // marca como lido pro afiliado
        ticketService.marcarComoLido(id, AfiliadoTicket.Autor.AFILIADO);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mensagens", mensagens);
        // ticket vem separado pra evitar reload lento
        return ResponseEntity.ok(out);
    }

    @PostMapping("/{id}/mensagens")
    public ResponseEntity<AfiliadoTicketMensagem> responder(@AuthenticationPrincipal String subject,
                                                             @PathVariable Long id,
                                                             @RequestBody Map<String, String> body) {
        Afiliado a = authService.afiliadoDoSubject(subject);
        return ResponseEntity.ok(
                ticketService.responderAfiliado(a, id, body.get("mensagem")));
    }

    @PostMapping("/{id}/fechar")
    public ResponseEntity<AfiliadoTicket> fechar(@AuthenticationPrincipal String subject,
                                                  @PathVariable Long id) {
        Afiliado a = authService.afiliadoDoSubject(subject);
        return ResponseEntity.ok(
                ticketService.fechar(id, a.getEmail(), AfiliadoTicket.Autor.AFILIADO));
    }
}

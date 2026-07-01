package com.myafiliados.service;

import com.myafiliados.model.Afiliado;
import com.myafiliados.model.AfiliadoEventoLog;
import com.myafiliados.model.AfiliadoTicket;
import com.myafiliados.model.AfiliadoTicketMensagem;
import com.myafiliados.repository.AfiliadoTicketMensagemRepository;
import com.myafiliados.repository.AfiliadoTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final AfiliadoTicketRepository ticketRepo;
    private final AfiliadoTicketMensagemRepository msgRepo;
    private final EventoLogService eventoLog;

    /** Afiliado abre novo ticket com uma mensagem inicial. */
    @Transactional
    public AfiliadoTicket abrir(Afiliado a, String assunto, String mensagemInicial) {
        if (assunto == null || assunto.trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assunto obrigatório");
        if (mensagemInicial == null || mensagemInicial.trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mensagem obrigatória");
        if (a.getStatus() != Afiliado.Status.APROVADO)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Só afiliados aprovados podem abrir chamados");

        AfiliadoTicket t = new AfiliadoTicket();
        t.setAfiliadoId(a.getId());
        t.setAfiliadoNome(a.getNome());
        t.setAfiliadoEmail(a.getEmail());
        t.setAssunto(assunto.trim());
        t.setStatus(AfiliadoTicket.Status.ABERTO);
        t.setUltimaRespostaPor(AfiliadoTicket.Autor.AFILIADO);
        t.setNaoLidoAdmin(true);
        t.setNaoLidoAfiliado(false);
        t = ticketRepo.save(t);

        AfiliadoTicketMensagem m = new AfiliadoTicketMensagem();
        m.setTicketId(t.getId());
        m.setAutor(AfiliadoTicket.Autor.AFILIADO);
        m.setAutorNome(a.getNome());
        m.setMensagem(mensagemInicial.trim());
        msgRepo.save(m);

        eventoLog.registrar(AfiliadoEventoLog.Tipo.TICKET_ABERTO,
                eventoLog.novo()
                        .afiliado(a.getId())
                        .ticket(t.getId())
                        .descricao("Novo ticket: " + assunto.trim()));
        return t;
    }

    /** Afiliado responde no próprio ticket. */
    @Transactional
    public AfiliadoTicketMensagem responderAfiliado(Afiliado a, Long ticketId, String mensagem) {
        AfiliadoTicket t = obterDoAfiliado(a, ticketId);
        if (t.getStatus() == AfiliadoTicket.Status.FECHADO)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ticket está fechado. Abra um novo se precisar.");
        return adicionarMensagem(t, AfiliadoTicket.Autor.AFILIADO, a.getNome(), mensagem);
    }

    /** Admin responde. */
    @Transactional
    public AfiliadoTicketMensagem responderAdmin(String adminEmail, Long ticketId, String mensagem) {
        AfiliadoTicket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (t.getStatus() == AfiliadoTicket.Status.FECHADO)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket está fechado");
        return adicionarMensagem(t, AfiliadoTicket.Autor.ADMIN, adminEmail, mensagem);
    }

    private AfiliadoTicketMensagem adicionarMensagem(AfiliadoTicket t,
                                                     AfiliadoTicket.Autor autor,
                                                     String autorNome, String mensagem) {
        if (mensagem == null || mensagem.trim().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mensagem vazia");

        AfiliadoTicketMensagem m = new AfiliadoTicketMensagem();
        m.setTicketId(t.getId());
        m.setAutor(autor);
        m.setAutorNome(autorNome);
        m.setMensagem(mensagem.trim());
        m = msgRepo.save(m);

        t.setUltimaRespostaPor(autor);
        if (autor == AfiliadoTicket.Autor.ADMIN) {
            t.setStatus(AfiliadoTicket.Status.RESPONDIDO);
            t.setNaoLidoAdmin(false);
            t.setNaoLidoAfiliado(true);
            eventoLog.registrar(AfiliadoEventoLog.Tipo.TICKET_RESPONDIDO_ADMIN,
                    eventoLog.novo()
                            .afiliado(t.getAfiliadoId())
                            .ticket(t.getId())
                            .admin(autorNome)
                            .descricao("Admin respondeu o ticket"));
        } else {
            if (t.getStatus() != AfiliadoTicket.Status.ABERTO
                    && t.getStatus() != AfiliadoTicket.Status.EM_ANALISE) {
                t.setStatus(AfiliadoTicket.Status.EM_ANALISE);
            }
            t.setNaoLidoAdmin(true);
            t.setNaoLidoAfiliado(false);
            eventoLog.registrar(AfiliadoEventoLog.Tipo.TICKET_RESPONDIDO_AFILIADO,
                    eventoLog.novo()
                            .afiliado(t.getAfiliadoId())
                            .ticket(t.getId())
                            .descricao("Afiliado respondeu o ticket"));
        }
        ticketRepo.save(t);
        return m;
    }

    /** Marca ticket como fechado (admin ou afiliado). */
    @Transactional
    public AfiliadoTicket fechar(Long ticketId, String fechadoPorEmail, AfiliadoTicket.Autor quem) {
        AfiliadoTicket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        t.setStatus(AfiliadoTicket.Status.FECHADO);
        t.setFechadoEm(LocalDateTime.now());
        t.setFechadoPorEmail(fechadoPorEmail);
        // marca ambos como lido pra sumir badge
        t.setNaoLidoAdmin(false);
        t.setNaoLidoAfiliado(false);
        ticketRepo.save(t);
        eventoLog.registrar(AfiliadoEventoLog.Tipo.TICKET_FECHADO,
                eventoLog.novo()
                        .afiliado(t.getAfiliadoId())
                        .ticket(t.getId())
                        .admin(quem == AfiliadoTicket.Autor.ADMIN ? fechadoPorEmail : null)
                        .descricao("Ticket fechado por " + quem));
        return t;
    }

    public List<AfiliadoTicket> listarDoAfiliado(Afiliado a) {
        return ticketRepo.findByAfiliadoIdOrderByAtualizadoEmDesc(a.getId());
    }

    public List<AfiliadoTicket> listarTodos() {
        return ticketRepo.findAllByOrderByAtualizadoEmDesc();
    }

    public List<AfiliadoTicketMensagem> mensagensDo(Long ticketId) {
        return msgRepo.findByTicketIdOrderByCriadoEmAsc(ticketId);
    }

    /** Marca ticket como lido pra quem estiver visualizando. */
    @Transactional
    public void marcarComoLido(Long ticketId, AfiliadoTicket.Autor porQuem) {
        AfiliadoTicket t = ticketRepo.findById(ticketId).orElse(null);
        if (t == null) return;
        if (porQuem == AfiliadoTicket.Autor.ADMIN) t.setNaoLidoAdmin(false);
        else t.setNaoLidoAfiliado(false);
        ticketRepo.save(t);
    }

    private AfiliadoTicket obterDoAfiliado(Afiliado a, Long ticketId) {
        AfiliadoTicket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!t.getAfiliadoId().equals(a.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ticket de outro afiliado");
        return t;
    }
}

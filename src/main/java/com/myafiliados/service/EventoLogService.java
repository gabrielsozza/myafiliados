package com.myafiliados.service;

import com.myafiliados.model.AfiliadoEventoLog;
import com.myafiliados.repository.AfiliadoEventoLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fachada pra registro de eventos de auditoria. Todos os pontos do sistema
 * usam este service — nunca escrevem direto no repository. Assim garante que
 * TODO evento passa pela mesma trilha de log e é imutável.
 *
 * Fail-safe: se por qualquer razão o registro falhar, apenas loga warning
 * — nunca propaga exception. Auditoria não pode ser causa de falha no fluxo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventoLogService {

    private final AfiliadoEventoLogRepository repo;

    public AfiliadoEventoLog registrar(AfiliadoEventoLog.Tipo tipo, Builder b) {
        try {
            AfiliadoEventoLog log = AfiliadoEventoLog.builder()
                    .tipo(tipo)
                    .afiliadoId(b.afiliadoId)
                    .vinculoId(b.vinculoId)
                    .restauranteId(b.restauranteId)
                    .comissaoId(b.comissaoId)
                    .ticketId(b.ticketId)
                    .codigoAfiliado(b.codigoAfiliado)
                    .ip(safe(b.ip, 60))
                    .userAgent(safe(b.userAgent, 500))
                    .detalhes(b.detalhes)
                    .descricao(safe(b.descricao, 500))
                    .adminEmail(safe(b.adminEmail, 200))
                    .build();
            return repo.save(log);
        } catch (Exception e) {
            log.warn("[EventoLog] falha ao registrar {}: {}", tipo, e.getMessage());
            return null;
        }
    }

    /** Helper pra iniciar um builder. */
    public Builder novo() { return new Builder(); }

    private static String safe(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** Helper — serializa Map em JSON simples (sem dependência de Jackson exposta). */
    public static String toJson(Map<String, Object> m) {
        if (m == null || m.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : m.entrySet()) {
            if (!first) sb.append(",");
            sb.append('"').append(escape(e.getKey())).append('"').append(":");
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else sb.append('"').append(escape(String.valueOf(v))).append('"');
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
    public static Map<String, Object> map() { return new LinkedHashMap<>(); }

    /** Builder fluent — evita ter 15 métodos com signatures diferentes. */
    public static class Builder {
        Long afiliadoId, vinculoId, restauranteId, comissaoId, ticketId;
        String codigoAfiliado, ip, userAgent, detalhes, descricao, adminEmail;
        public Builder afiliado(Long id) { this.afiliadoId = id; return this; }
        public Builder vinculo(Long id) { this.vinculoId = id; return this; }
        public Builder restaurante(Long id) { this.restauranteId = id; return this; }
        public Builder comissao(Long id) { this.comissaoId = id; return this; }
        public Builder ticket(Long id) { this.ticketId = id; return this; }
        public Builder codigoAfiliado(String c) { this.codigoAfiliado = c; return this; }
        public Builder ip(String i) { this.ip = i; return this; }
        public Builder userAgent(String u) { this.userAgent = u; return this; }
        public Builder detalhes(String d) { this.detalhes = d; return this; }
        public Builder detalhes(Map<String, Object> d) { this.detalhes = toJson(d); return this; }
        public Builder descricao(String d) { this.descricao = d; return this; }
        public Builder admin(String email) { this.adminEmail = email; return this; }
    }
}

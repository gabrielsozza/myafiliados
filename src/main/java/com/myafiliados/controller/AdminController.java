package com.myafiliados.controller;

import com.myafiliados.dto.Dtos;
import com.myafiliados.model.*;
import com.myafiliados.repository.*;
import com.myafiliados.service.AfiliadoService;
import com.myafiliados.service.ComissaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Endpoints do admin (role ADMIN_AFILIADOS). Acessados pelo painel
 * admin do MyDelivery via JWT do próprio painel de afiliados.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN_AFILIADOS')")
@RequiredArgsConstructor
public class AdminController {

    private final AfiliadoRepository afiliadoRepo;
    private final AfiliadoVinculoRepository vinculoRepo;
    private final AfiliadoComissaoRepository comissaoRepo;
    private final CursoModuloRepository cursoRepo;
    private final CursoGrupoRepository grupoRepo;
    private final MaterialApoioRepository materialRepo;
    private final AfiliadoService afiliadoService;
    private final ComissaoService comissaoService;

    // ───────────────────────── AFILIADOS ─────────────────────────

    @GetMapping("/afiliados")
    public ResponseEntity<List<Map<String, Object>>> listar(@RequestParam(required = false) String status) {
        List<Afiliado> lista = status != null && !status.isBlank()
                ? afiliadoRepo.findByStatusOrderByCriadoEmDesc(Afiliado.Status.valueOf(status.toUpperCase()))
                : afiliadoRepo.findAll();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Afiliado a : lista) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("codigo", a.getCodigo());
            m.put("nome", a.getNome());
            m.put("email", a.getEmail());
            m.put("telefone", a.getTelefone());
            m.put("status", a.getStatus().name());
            m.put("comissaoPercentual", a.getComissaoPercentual());
            m.put("indicados", vinculoRepo.countByAfiliadoId(a.getId()));
            m.put("ativos", vinculoRepo.countByAfiliadoIdAndStatusVinculo(a.getId(), AfiliadoVinculo.StatusVinculo.ATIVO));
            m.put("criadoEm", a.getCriadoEm());
            m.put("aprovadoEm", a.getAprovadoEm());
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/afiliados/{id}")
    public ResponseEntity<Map<String, Object>> detalhe(@PathVariable Long id) {
        Afiliado a = afiliadoRepo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("afiliado", Dtos.AfiliadoResponse.de(a));
        out.put("vinculos", vinculoRepo.findByAfiliadoIdOrderByCriadoEmDesc(id));
        out.put("comissoes", comissaoRepo.findByAfiliadoIdOrderByMesReferenciaDesc(id));
        out.put("dashboard", comissaoService.dashboard(id));
        return ResponseEntity.ok(out);
    }

    @PostMapping("/afiliados/{id}/aprovar")
    public ResponseEntity<Dtos.AfiliadoResponse> aprovar(@PathVariable Long id,
                                                         @AuthenticationPrincipal String subject) {
        Afiliado a = afiliadoService.aprovar(id, subjectEmail(subject));
        return ResponseEntity.ok(Dtos.AfiliadoResponse.de(a));
    }

    @PostMapping("/afiliados/{id}/bloquear")
    public ResponseEntity<Dtos.AfiliadoResponse> bloquear(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body,
                                                          @AuthenticationPrincipal String subject) {
        Afiliado a = afiliadoService.bloquear(id, subjectEmail(subject), body.getOrDefault("motivo", null));
        return ResponseEntity.ok(Dtos.AfiliadoResponse.de(a));
    }

    @PostMapping("/afiliados/{id}/desbloquear")
    public ResponseEntity<Dtos.AfiliadoResponse> desbloquear(@PathVariable Long id) {
        Afiliado a = afiliadoService.desbloquear(id);
        return ResponseEntity.ok(Dtos.AfiliadoResponse.de(a));
    }

    @PatchMapping("/afiliados/{id}/comissao")
    public ResponseEntity<Dtos.AfiliadoResponse> ajustarComissao(@PathVariable Long id,
                                                                 @RequestBody Dtos.AjustarComissaoRequest req) {
        return ResponseEntity.ok(Dtos.AfiliadoResponse.de(afiliadoService.ajustarComissao(id, req.percentual)));
    }

    @PatchMapping("/afiliados/{id}/senha")
    public ResponseEntity<Map<String, Object>> alterarSenha(@PathVariable Long id,
                                                             @RequestBody Map<String, String> body) {
        afiliadoService.alterarSenhaAdmin(id, body.get("novaSenha"));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * Apaga afiliado E todos os dados relacionados (vínculos + comissões).
     * Operação destrutiva — sem undo.
     */
    @DeleteMapping("/afiliados/{id}")
    public ResponseEntity<Map<String, Object>> excluir(@PathVariable Long id) {
        // Comissões primeiro (FK lógica → vinculo)
        comissaoRepo.deleteAll(comissaoRepo.findByAfiliadoIdOrderByMesReferenciaDesc(id));
        vinculoRepo.deleteAll(vinculoRepo.findByAfiliadoIdOrderByCriadoEmDesc(id));
        afiliadoRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ───────────────────────── COMISSÕES ─────────────────────────

    @GetMapping("/comissoes/pendentes")
    public ResponseEntity<List<AfiliadoComissao>> comissoesPendentes() {
        return ResponseEntity.ok(
                comissaoRepo.findByStatusOrderByMesReferenciaAscAfiliadoIdAsc(AfiliadoComissao.Status.PENDENTE));
    }

    @PostMapping("/comissoes/{id}/pagar")
    public ResponseEntity<AfiliadoComissao> pagarComissao(@PathVariable Long id,
                                                           @RequestBody Dtos.PagarComissaoRequest req,
                                                           @AuthenticationPrincipal String subject) {
        if (req == null) req = new Dtos.PagarComissaoRequest();
        if (req.adminEmail == null) req.adminEmail = subjectEmail(subject);
        return ResponseEntity.ok(comissaoService.marcarComoPaga(id, req));
    }

    @PostMapping("/comissoes/{id}/cancelar")
    public ResponseEntity<AfiliadoComissao> cancelarComissao(@PathVariable Long id,
                                                              @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(comissaoService.cancelar(id, body.getOrDefault("motivo", null)));
    }

    /** Dispara manualmente a geração do mês corrente (admin). Job mensal automático também roda. */
    @PostMapping("/comissoes/gerar-mes-atual")
    public ResponseEntity<Map<String, Object>> gerarMesAtual() {
        int n = comissaoService.gerarComissoesDoMes(java.time.YearMonth.now());
        return ResponseEntity.ok(Map.of("criadas", n));
    }

    // ───────────────────────── CURSOS ─────────────────────────

    @GetMapping("/cursos")
    public ResponseEntity<List<CursoModulo>> listarCursos() {
        return ResponseEntity.ok(cursoRepo.findAllByOrderByOrdemAscIdAsc());
    }

    @PostMapping("/cursos")
    public ResponseEntity<CursoModulo> criarCurso(@RequestBody Dtos.CursoModuloRequest req) {
        CursoModulo m = new CursoModulo();
        return ResponseEntity.ok(cursoRepo.save(aplicar(m, req)));
    }

    @PutMapping("/cursos/{id}")
    public ResponseEntity<CursoModulo> editarCurso(@PathVariable Long id,
                                                    @RequestBody Dtos.CursoModuloRequest req) {
        CursoModulo m = cursoRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(cursoRepo.save(aplicar(m, req)));
    }

    @DeleteMapping("/cursos/{id}")
    public ResponseEntity<Void> apagarCurso(@PathVariable Long id) {
        cursoRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private CursoModulo aplicar(CursoModulo m, Dtos.CursoModuloRequest r) {
        if (r.titulo != null) m.setTitulo(r.titulo);
        if (r.descricao != null) m.setDescricao(r.descricao);
        if (r.videoUrl != null) m.setVideoUrl(normalizarVideoUrl(r.videoUrl));
        if (r.ordem != null) m.setOrdem(r.ordem);
        if (r.ativo != null) m.setAtivo(r.ativo);
        if (r.duracaoSegundos != null) m.setDuracaoSegundos(r.duracaoSegundos);
        // grupoId: aceita explicit null pra "desagrupar"
        m.setGrupoId(r.grupoId);
        if (r.anexosJson != null) m.setAnexosJson(r.anexosJson.isBlank() ? null : r.anexosJson);
        return m;
    }

    /**
     * Normaliza URL do YouTube pra formato de embed. Aceita:
     *  - youtube.com/watch?v=ID
     *  - youtu.be/ID
     *  - youtube.com/embed/ID (já normalizado)
     *  - youtube.com/shorts/ID
     * Qualquer outra URL (Cloudinary, Vimeo, arquivo mp4) passa direto.
     */
    private String normalizarVideoUrl(String url) {
        if (url == null) return null;
        String u = url.trim();
        if (u.isEmpty()) return u;
        // watch?v=
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/shorts/)([A-Za-z0-9_-]{6,})")
                .matcher(u);
        if (m.find()) {
            return "https://www.youtube.com/embed/" + m.group(1);
        }
        return u;
    }

    // ───────────────────────── GRUPOS DE CURSO ─────────────────────────

    @GetMapping("/curso-grupos")
    public ResponseEntity<List<CursoGrupo>> listarGrupos() {
        return ResponseEntity.ok(grupoRepo.findAllByOrderByOrdemAscIdAsc());
    }

    @PostMapping("/curso-grupos")
    public ResponseEntity<CursoGrupo> criarGrupo(@RequestBody Dtos.CursoGrupoRequest req) {
        return ResponseEntity.ok(grupoRepo.save(aplicarGrupo(new CursoGrupo(), req)));
    }

    @PutMapping("/curso-grupos/{id}")
    public ResponseEntity<CursoGrupo> editarGrupo(@PathVariable Long id,
                                                    @RequestBody Dtos.CursoGrupoRequest req) {
        CursoGrupo g = grupoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(grupoRepo.save(aplicarGrupo(g, req)));
    }

    @DeleteMapping("/curso-grupos/{id}")
    public ResponseEntity<Void> apagarGrupo(@PathVariable Long id) {
        // Aulas do grupo NÃO são apagadas — só perdem o vínculo (grupoId=null).
        cursoRepo.findAll().stream()
                .filter(c -> id.equals(c.getGrupoId()))
                .forEach(c -> { c.setGrupoId(null); cursoRepo.save(c); });
        grupoRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private CursoGrupo aplicarGrupo(CursoGrupo g, Dtos.CursoGrupoRequest r) {
        if (r.titulo != null) g.setTitulo(r.titulo);
        if (r.descricao != null) g.setDescricao(r.descricao);
        if (r.ordem != null) g.setOrdem(r.ordem);
        if (r.ativo != null) g.setAtivo(r.ativo);
        return g;
    }

    // ───────────────────────── MATERIAIS ─────────────────────────

    @GetMapping("/materiais")
    public ResponseEntity<List<MaterialApoio>> listarMateriais() {
        return ResponseEntity.ok(materialRepo.findAllByOrderByOrdemAscIdAsc());
    }

    @PostMapping("/materiais")
    public ResponseEntity<MaterialApoio> criarMaterial(@RequestBody Dtos.MaterialApoioRequest req) {
        return ResponseEntity.ok(materialRepo.save(aplicar(new MaterialApoio(), req)));
    }

    @PutMapping("/materiais/{id}")
    public ResponseEntity<MaterialApoio> editarMaterial(@PathVariable Long id,
                                                         @RequestBody Dtos.MaterialApoioRequest req) {
        MaterialApoio m = materialRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(materialRepo.save(aplicar(m, req)));
    }

    @DeleteMapping("/materiais/{id}")
    public ResponseEntity<Void> apagarMaterial(@PathVariable Long id) {
        materialRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private MaterialApoio aplicar(MaterialApoio m, Dtos.MaterialApoioRequest r) {
        if (r.titulo != null) m.setTitulo(r.titulo);
        if (r.descricao != null) m.setDescricao(r.descricao);
        if (r.arquivoUrl != null) m.setArquivoUrl(r.arquivoUrl);
        if (r.tipo != null) {
            try { m.setTipo(MaterialApoio.Tipo.valueOf(r.tipo.toUpperCase())); } catch (Exception ignored) {}
        }
        if (r.ordem != null) m.setOrdem(r.ordem);
        if (r.ativo != null) m.setAtivo(r.ativo);
        if (r.tamanhoBytes != null) m.setTamanhoBytes(r.tamanhoBytes);
        return m;
    }

    // ───────────────────────── helpers ─────────────────────────

    private String subjectEmail(String subject) {
        if (subject == null) return null;
        return subject.startsWith("admin:") ? subject.substring(6) : subject;
    }
}

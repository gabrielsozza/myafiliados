package com.myafiliados.controller;

import com.myafiliados.model.Afiliado;
import com.myafiliados.model.CursoGrupo;
import com.myafiliados.model.CursoModulo;
import com.myafiliados.model.MaterialApoio;
import com.myafiliados.repository.AfiliadoRepository;
import com.myafiliados.repository.CursoGrupoRepository;
import com.myafiliados.repository.CursoModuloRepository;
import com.myafiliados.repository.MaterialApoioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Endpoints públicos OU acessíveis pelo afiliado autenticado.
 *
 *  GET /api/public/afiliado/{codigo}/existe
 *      - Usado pela landing pra verificar se o ?afiliado=XXX é válido
 *        antes de mostrar o badge "indicado por: NOME".
 *
 *  GET /api/afiliado/cursos / materiais — só afiliado autenticado vê.
 */
@RestController
@RequiredArgsConstructor
public class PublicController {

    private final AfiliadoRepository afiliadoRepo;
    private final CursoModuloRepository cursoRepo;
    private final CursoGrupoRepository grupoRepo;
    private final MaterialApoioRepository materialRepo;

    @GetMapping("/api/public/afiliado/{codigo}/existe")
    public ResponseEntity<Map<String, Object>> existeCodigo(@PathVariable String codigo) {
        return afiliadoRepo.findByCodigo(codigo)
                .filter(a -> a.getStatus() == Afiliado.Status.APROVADO)
                .<Map<String, Object>>map(a -> Map.of(
                        "existe", (Object) true, "nome", a.getNome(), "codigo", a.getCodigo()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("existe", false)));
    }

    @GetMapping("/api/afiliado/cursos")
    @PreAuthorize("hasRole('AFILIADO')")
    public ResponseEntity<List<CursoModulo>> cursos() {
        return ResponseEntity.ok(cursoRepo.findByAtivoTrueOrderByOrdemAscIdAsc());
    }

    /**
     * Estrutura organizada em módulos → aulas. Frontend do afiliado usa
     * esse endpoint pra montar "Módulo 1 (3 aulas) → aula, aula, aula".
     */
    @GetMapping("/api/afiliado/cursos-organizados")
    @PreAuthorize("hasRole('AFILIADO')")
    public ResponseEntity<List<Map<String, Object>>> cursosOrganizados() {
        List<CursoGrupo> grupos = grupoRepo.findByAtivoTrueOrderByOrdemAscIdAsc();
        List<CursoModulo> aulas = cursoRepo.findByAtivoTrueOrderByOrdemAscIdAsc();
        Map<Long, List<CursoModulo>> porGrupo = aulas.stream()
                .filter(a -> a.getGrupoId() != null)
                .collect(Collectors.groupingBy(CursoModulo::getGrupoId));
        List<CursoModulo> semGrupo = aulas.stream()
                .filter(a -> a.getGrupoId() == null)
                .collect(Collectors.toList());

        List<Map<String, Object>> out = new ArrayList<>();
        for (CursoGrupo g : grupos) {
            List<CursoModulo> list = porGrupo.getOrDefault(g.getId(), List.of());
            if (list.isEmpty()) continue; // grupo vazio some da UI do afiliado
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("titulo", g.getTitulo());
            m.put("descricao", g.getDescricao());
            m.put("ordem", g.getOrdem());
            m.put("aulas", list);
            out.add(m);
        }
        if (!semGrupo.isEmpty()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", null);
            m.put("titulo", "Sem categoria");
            m.put("descricao", "Aulas ainda não organizadas em módulos.");
            m.put("ordem", 999);
            m.put("aulas", semGrupo);
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/api/afiliado/materiais")
    @PreAuthorize("hasRole('AFILIADO')")
    public ResponseEntity<List<MaterialApoio>> materiais() {
        return ResponseEntity.ok(materialRepo.findByAtivoTrueOrderByOrdemAscIdAsc());
    }
}

package com.myafiliados.controller;

import com.myafiliados.model.Afiliado;
import com.myafiliados.model.CursoModulo;
import com.myafiliados.model.MaterialApoio;
import com.myafiliados.repository.AfiliadoRepository;
import com.myafiliados.repository.CursoModuloRepository;
import com.myafiliados.repository.MaterialApoioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/api/afiliado/materiais")
    @PreAuthorize("hasRole('AFILIADO')")
    public ResponseEntity<List<MaterialApoio>> materiais() {
        return ResponseEntity.ok(materialRepo.findByAtivoTrueOrderByOrdemAscIdAsc());
    }
}

package com.myafiliados.controller;

import com.myafiliados.dto.Dtos;
import com.myafiliados.model.Afiliado;
import com.myafiliados.model.AfiliadoVinculo;
import com.myafiliados.repository.AfiliadoComissaoRepository;
import com.myafiliados.repository.AfiliadoVinculoRepository;
import com.myafiliados.service.AfiliadoAuthService;
import com.myafiliados.service.AfiliadoService;
import com.myafiliados.service.ComissaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints do PRÓPRIO afiliado autenticado. Tudo escopado pelo subject do JWT.
 */
@RestController
@RequestMapping("/api/afiliado")
@PreAuthorize("hasRole('AFILIADO')")
@RequiredArgsConstructor
public class AfiliadoController {

    private final AfiliadoAuthService authService;
    private final AfiliadoService afiliadoService;
    private final ComissaoService comissaoService;
    private final AfiliadoVinculoRepository vinculoRepo;
    private final AfiliadoComissaoRepository comissaoRepo;

    @Value("${mydelivery.afiliados.checkout-base-url:https://mydeliveryfood.com.br}")
    private String checkoutBaseUrl;

    @GetMapping("/me")
    public ResponseEntity<Dtos.AfiliadoResponse> me(@AuthenticationPrincipal String subject) {
        Afiliado a = authService.afiliadoDoSubject(subject);
        return ResponseEntity.ok(Dtos.AfiliadoResponse.de(a));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Dtos.DashboardResponse> dashboard(@AuthenticationPrincipal String subject) {
        Afiliado a = authService.afiliadoDoSubject(subject);
        return ResponseEntity.ok(comissaoService.dashboard(a.getId()));
    }

    @GetMapping("/links")
    public ResponseEntity<Map<String, Object>> links(@AuthenticationPrincipal String subject) {
        Afiliado a = authService.afiliadoDoSubject(subject);
        String base = checkoutBaseUrl.replaceAll("/$", "");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("codigo", a.getCodigo());
        // URL correta: login.html com hash #cadastro (a landing captura ?afiliado=
        // e ?plano= no localStorage já no primeiro load — ver login.html)
        out.put("mensal", base + "/login.html?afiliado=" + a.getCodigo() + "&plano=mensal#cadastro");
        out.put("semestral", base + "/login.html?afiliado=" + a.getCodigo() + "&plano=semestral#cadastro");
        out.put("anual", base + "/login.html?afiliado=" + a.getCodigo() + "&plano=anual#cadastro");
        out.put("generico", base + "/?afiliado=" + a.getCodigo());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/vinculos")
    public ResponseEntity<List<AfiliadoVinculo>> meusVinculos(@AuthenticationPrincipal String subject) {
        Afiliado a = authService.afiliadoDoSubject(subject);
        return ResponseEntity.ok(vinculoRepo.findByAfiliadoIdOrderByCriadoEmDesc(a.getId()));
    }

    @GetMapping("/comissoes")
    public ResponseEntity<List<com.myafiliados.model.AfiliadoComissao>> minhasComissoes(
            @AuthenticationPrincipal String subject) {
        Afiliado a = authService.afiliadoDoSubject(subject);
        return ResponseEntity.ok(comissaoRepo.findByAfiliadoIdOrderByMesReferenciaDesc(a.getId()));
    }

    @PutMapping("/dados-pix")
    public ResponseEntity<Dtos.AfiliadoResponse> atualizarPix(@AuthenticationPrincipal String subject,
                                                              @RequestBody Dtos.DadosPixRequest req) {
        Afiliado a = authService.afiliadoDoSubject(subject);
        Afiliado salvo = afiliadoService.atualizarDadosPix(a.getId(), req);
        return ResponseEntity.ok(Dtos.AfiliadoResponse.de(salvo));
    }
}

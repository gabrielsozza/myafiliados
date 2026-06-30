package com.myafiliados.service;

import com.myafiliados.dto.Dtos;
import com.myafiliados.model.Afiliado;
import com.myafiliados.model.AfiliadoComissao;
import com.myafiliados.model.AfiliadoVinculo;
import com.myafiliados.repository.AfiliadoComissaoRepository;
import com.myafiliados.repository.AfiliadoRepository;
import com.myafiliados.repository.AfiliadoVinculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Lógica de comissão.
 *
 * Regra MENSALIZADA:
 *   valorMensalEquivalente × percentualAfiliado / 100 = comissão do mês
 *
 *   Ex: Anual R$550 / 12 = R$45,83/mês × 33,33% = R$15,28/mês
 *
 * O valorMensalEquivalente é calculado e armazenado no vínculo quando o
 * restaurante assina (webhook), pra ser snapshot estável.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComissaoService {

    private final AfiliadoRepository afiliadoRepo;
    private final AfiliadoVinculoRepository vinculoRepo;
    private final AfiliadoComissaoRepository comissaoRepo;

    /** Roda no job mensal — gera comissões pra todos os vínculos ATIVOS no mês de referência. */
    @Transactional
    public int gerarComissoesDoMes(YearMonth mes) {
        String mesRef = mes.toString(); // "YYYY-MM"
        List<AfiliadoVinculo> ativos = vinculoRepo.findByStatusVinculo(AfiliadoVinculo.StatusVinculo.ATIVO);
        int criadas = 0;
        for (AfiliadoVinculo v : ativos) {
            if (v.getValorMensalEquivalente() == null) continue;
            // Idempotência: se já tem comissão pra esse vinculo+mes, pula
            if (comissaoRepo.findByVinculoIdAndMesReferencia(v.getId(), mesRef).isPresent()) continue;
            Afiliado a = afiliadoRepo.findById(v.getAfiliadoId()).orElse(null);
            if (a == null) continue;
            BigDecimal valor = v.getValorMensalEquivalente()
                    .multiply(a.getComissaoPercentual())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            AfiliadoComissao c = new AfiliadoComissao();
            c.setAfiliadoId(a.getId());
            c.setVinculoId(v.getId());
            c.setMesReferencia(mesRef);
            c.setValor(valor);
            c.setPercentualAplicado(a.getComissaoPercentual());
            c.setValorMensalEquivalente(v.getValorMensalEquivalente());
            c.setStatus(AfiliadoComissao.Status.PENDENTE);
            comissaoRepo.save(c);
            criadas++;
        }
        log.info("[Comissao] mês={} criadas={}", mesRef, criadas);
        return criadas;
    }

    @Transactional
    public AfiliadoComissao marcarComoPaga(Long comissaoId, Dtos.PagarComissaoRequest req) {
        AfiliadoComissao c = comissaoRepo.findById(comissaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (c.getStatus() == AfiliadoComissao.Status.PAGA) return c;
        c.setStatus(AfiliadoComissao.Status.PAGA);
        c.setPagoEm(LocalDateTime.now());
        c.setPagoPor(req != null ? req.adminEmail : null);
        c.setObservacao(req != null ? req.observacao : null);
        return comissaoRepo.save(c);
    }

    @Transactional
    public AfiliadoComissao cancelar(Long comissaoId, String motivo) {
        AfiliadoComissao c = comissaoRepo.findById(comissaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.setStatus(AfiliadoComissao.Status.CANCELADA);
        c.setObservacao(motivo);
        return comissaoRepo.save(c);
    }

    /** Dashboard do afiliado: agregados. */
    public Dtos.DashboardResponse dashboard(Long afiliadoId) {
        Dtos.DashboardResponse r = new Dtos.DashboardResponse();
        r.indicados = vinculoRepo.countByAfiliadoId(afiliadoId);
        r.emTrial = vinculoRepo.countByAfiliadoIdAndStatusVinculo(afiliadoId, AfiliadoVinculo.StatusVinculo.TRIAL);
        r.ativos = vinculoRepo.countByAfiliadoIdAndStatusVinculo(afiliadoId, AfiliadoVinculo.StatusVinculo.ATIVO);
        r.cancelados = vinculoRepo.countByAfiliadoIdAndStatusVinculo(afiliadoId, AfiliadoVinculo.StatusVinculo.CANCELADO);
        r.trialExpirado = vinculoRepo.countByAfiliadoIdAndStatusVinculo(afiliadoId, AfiliadoVinculo.StatusVinculo.TRIAL_EXPIRADO);

        long converteram = r.ativos + r.cancelados;
        long expostos = converteram + r.emTrial + r.trialExpirado;
        if (expostos > 0) {
            r.taxaConversao = new BigDecimal(converteram)
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(expostos), 1, RoundingMode.HALF_UP);
        } else {
            r.taxaConversao = BigDecimal.ZERO;
        }

        BigDecimal pagas = comissaoRepo.somaPorAfiliadoEStatus(afiliadoId, AfiliadoComissao.Status.PAGA);
        BigDecimal pendentes = comissaoRepo.somaPorAfiliadoEStatus(afiliadoId, AfiliadoComissao.Status.PENDENTE);
        r.comissoesPagas = pagas != null ? pagas : BigDecimal.ZERO;
        r.comissoesPendentes = pendentes != null ? pendentes : BigDecimal.ZERO;
        r.comissoesGeradas = r.comissoesPagas.add(r.comissoesPendentes);
        return r;
    }
}

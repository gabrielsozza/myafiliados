package com.myafiliados.service;

import com.myafiliados.model.Afiliado;
import com.myafiliados.model.AfiliadoComissao;
import com.myafiliados.model.AfiliadoVinculo;
import com.myafiliados.repository.AfiliadoComissaoRepository;
import com.myafiliados.repository.AfiliadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * Gera comissão IMEDIATAMENTE quando o webhook ASSINOU chega.
 *
 * Regra:
 *  - Comissão nasce como PENDENTE (aguardando admin pagar via PIX manual)
 *  - Valor = valorMensalEquivalente × percentualAfiliado
 *  - Mês de referência = mês corrente da assinatura
 *  - IDEMPOTENTE: unique constraint (vinculo_id, mes_referencia) impede
 *    duplicação se o webhook chegar mais de uma vez
 *
 * Se o restaurante cancelar/estornar depois, o admin cancela manualmente
 * pelo painel de auditoria — não deixa dinheiro sair sem confirmação
 * humana. Isso é intencional: pagamento é manual, então cancelamento
 * também é manual.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoComissaoService {

    private final AfiliadoRepository afiliadoRepo;
    private final AfiliadoComissaoRepository comissaoRepo;

    @Transactional
    public AfiliadoComissao gerarComissaoAssinatura(AfiliadoVinculo v) {
        if (v == null || v.getAfiliadoId() == null) return null;
        if (v.getValorMensalEquivalente() == null
                || v.getValorMensalEquivalente().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[AutoComissao] vinculo={} sem valorMensalEquivalente — pula", v.getId());
            return null;
        }
        Afiliado a = afiliadoRepo.findById(v.getAfiliadoId()).orElse(null);
        if (a == null) {
            log.warn("[AutoComissao] afiliado {} não existe", v.getAfiliadoId());
            return null;
        }

        String mes = YearMonth.now().toString(); // "2026-07"

        // Idempotência: se já existe comissão desse mês pra esse vínculo, retorna a existente
        var existente = comissaoRepo.findByVinculoIdAndMesReferencia(v.getId(), mes);
        if (existente.isPresent()) {
            log.info("[AutoComissao] vínculo={} mês={} já tinha comissão (id={}) — retornando",
                    v.getId(), mes, existente.get().getId());
            return existente.get();
        }

        BigDecimal percentual = a.getComissaoPercentual() != null
                ? a.getComissaoPercentual()
                : new BigDecimal("33.33");

        BigDecimal valor = v.getValorMensalEquivalente()
                .multiply(percentual)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        AfiliadoComissao c = new AfiliadoComissao();
        c.setAfiliadoId(a.getId());
        c.setVinculoId(v.getId());
        c.setMesReferencia(mes);
        c.setValor(valor);
        c.setPercentualAplicado(percentual);
        c.setValorMensalEquivalente(v.getValorMensalEquivalente());
        c.setStatus(AfiliadoComissao.Status.PENDENTE);
        c.setObservacao("Gerada automaticamente na assinatura");
        AfiliadoComissao salva = comissaoRepo.save(c);
        log.info("[AutoComissao] gerou comissão id={} afiliado={} valor={}",
                salva.getId(), a.getId(), valor);
        return salva;
    }
}

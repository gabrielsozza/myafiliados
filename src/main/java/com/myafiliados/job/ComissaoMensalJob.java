package com.myafiliados.job;

import com.myafiliados.service.ComissaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * Job que roda dia 5 às 03:00 e gera as comissões do mês corrente
 * pra todos os vínculos ATIVOS.
 *
 * Idempotente: já cuida via unique key (vinculo, mês). Pode rodar várias
 * vezes sem duplicar.
 *
 * O admin também pode disparar manualmente via POST /api/admin/comissoes/gerar-mes-atual.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComissaoMensalJob {

    private final ComissaoService service;

    @Scheduled(cron = "0 0 3 5 * *")
    public void rodar() {
        try {
            int n = service.gerarComissoesDoMes(YearMonth.now());
            log.info("[ComissaoMensalJob] criadas={}", n);
        } catch (Exception e) {
            log.error("[ComissaoMensalJob] erro: {}", e.getMessage(), e);
        }
    }
}

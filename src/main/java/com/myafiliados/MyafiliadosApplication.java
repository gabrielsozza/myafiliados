package com.myafiliados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * myDelivery Afiliados — API standalone.
 *
 * Banco: myafiliados (MySQL).
 * Comunicação com mydelivery-api: via webhook (entrada) e HTTP signed (saída).
 *
 * Decisões de arquitetura (V1):
 *  - Banco SEPARADO do mydelivery (escolha do user em decisões iniciais).
 *  - Comissão MENSALIZADA mesmo pra planos semestral/anual (parcela proporcional).
 *  - Pagamento de comissão é MANUAL pelo admin — sistema só registra status.
 *  - JWT próprio (com role AFILIADO ou ADMIN_AFILIADOS).
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class MyafiliadosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyafiliadosApplication.class, args);
    }
}

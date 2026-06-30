# myDelivery Afiliados — API

API standalone do programa de afiliados do MyDelivery.

## Stack
- Java 21 + Spring Boot 3.5
- MySQL 8 (banco `myafiliados`)
- Lombok + JWT (jjwt 0.12.6)
- Mesma arquitetura do `mydelivery-api`

## Estrutura

```
myafiliados-api/
├── pom.xml
└── src/main/
    ├── java/com/myafiliados/
    │   ├── MyafiliadosApplication.java
    │   ├── model/                  ← ETAPA 1 ✅
    │   │   ├── Afiliado.java
    │   │   ├── AfiliadoVinculo.java
    │   │   ├── AfiliadoComissao.java
    │   │   ├── CursoModulo.java
    │   │   ├── MaterialApoio.java
    │   │   └── ConfigGlobal.java
    │   ├── repository/             ← ETAPA 1 ✅
    │   │   ├── AfiliadoRepository.java
    │   │   ├── AfiliadoVinculoRepository.java
    │   │   ├── AfiliadoComissaoRepository.java
    │   │   ├── CursoModuloRepository.java
    │   │   ├── MaterialApoioRepository.java
    │   │   └── ConfigGlobalRepository.java
    │   ├── config/                 ← ETAPA 2 (Security/CORS)
    │   ├── controller/             ← ETAPA 2 (REST)
    │   ├── service/                ← ETAPA 2 (lógica)
    │   ├── dto/                    ← ETAPA 2
    │   ├── security/               ← ETAPA 2 (JWT)
    │   └── job/                    ← ETAPA 2 (geração mensal)
    └── resources/
        └── application.properties
```

## Modelo de dados (Etapa 1)

| Tabela | Pra que serve |
|--------|---------------|
| `afiliados` | Conta do afiliado, dados de PIX, % comissão, status (PENDENTE/APROVADO/BLOQUEADO/INATIVO) |
| `afiliado_vinculos` | Link entre afiliado e restaurante (1 restaurante = 1 afiliado max) |
| `afiliado_comissoes` | 1 registro por mês × vínculo ativo (idempotente via unique key) |
| `curso_modulos` | Vídeo-aulas internas pro afiliado |
| `materiais_apoio` | PDF de apresentação, kit de mídia, etc |
| `configs` | Chave-valor pra parametrização runtime (% padrão, dia do mês p/ gerar comissões, etc) |

## Decisões de arquitetura registradas

1. **Banco separado** (`myafiliados`) — isolamento total do `mydelivery_db`.
2. **Comissão MENSALIZADA** — anual R$550 vira 12 parcelas de R$15,28 (proporcional ao mês recebido).
3. **Pagamento manual** — sistema só registra status (PENDENTE/PAGA), admin faz o PIX e marca.
4. **Comunicação cross-service via webhook** — `mydelivery-api` notifica `myafiliados-api` quando restaurante vinculado cria conta/assina/cancela.

## Pré-requisitos pra rodar local

```bash
# 1. Criar banco no MySQL local
mysql -u root -e "CREATE DATABASE IF NOT EXISTS myafiliados;"

# 2. Configurar .env na raiz do myafiliados-api (não commitar):
JWT_SECRET_AFILIADOS=<base64 com pelo menos 256 bits>
SMTP_PASSWORD=tznj klwm eiru kgbi
# (DB_URL_AFILIADOS, DB_USERNAME_AFILIADOS, DB_PASSWORD_AFILIADOS opcionais — default localhost)

# 3. Rodar
mvn spring-boot:run
# App sobe na porta 8090 (mydelivery-api fica em 8080 — sem colisão)
```

## Próximas etapas
- **ETAPA 2**: Backend REST (auth, signup, dashboard, links, webhook inbound)
- **ETAPA 3**: Frontend `/myafiliados` (HTML/CSS/JS vanilla, dark mode profissional)
- **ETAPA 4**: Integração com painel admin do MyDelivery (aba "Afiliados")
- **ETAPA 5**: Mudanças no `mydelivery-api` (campo `afiliado_codigo` no Restaurante, webhook outbound, preços personalizados)
- **ETAPA 6**: Testes E2E + landing afiliado.html + alteração landing pra R$75

# service-track-checkout

Microsserviço de **cobrança** da plataforma ServiceTrack. Cobra uma ordem de serviço por **Pix,
boleto ou cartão** através do Mercado Pago, e é o único lugar do sistema que conversa com um
provedor de pagamento.

**A integração usa a API de Orders do Mercado Pago** (`POST /v1/orders`), e não a Payments API
(`/v1/payments`): com credencial de teste de conta nova, a Payments API recusa toda cobrança.
Decisão e medição em [`CHK-ADR-001`](docs/adr/CHK-ADR-001-api-de-orders-do-mercado-pago.md).

---

## O que este serviço expõe

| Método | Rota | Para quê |
|---|---|---|
| `POST` | `/cobrancas` | solicita a cobrança de uma ordem de serviço |
| `GET` | `/cobrancas/{id}` | consulta uma cobrança |
| `GET` | `/cobrancas?ordemServicoId=` | cobranças de uma ordem |
| `POST` | `/cobrancas/{id}/reconciliacao` | pergunta ao provedor e aplica o que ele reporta |
| `POST` | `/webhooks/mercado-pago` | recebe a notificação do provedor |

Documentação em `/swagger-ui.html`. **O contrato vive nos ports de entrada**, não nos
controllers: quem lê o contrato lê uma interface.

### Pix e boleto são assíncronos; cartão não é

| Meio | A resposta de `POST /cobrancas` | Como a aprovação chega |
|---|---|---|
| Pix | `PENDENTE`, com `qrCode` e `qrCodeBase64` | notificação do provedor |
| Boleto | `PENDENTE`, com `linkDoBoleto` | notificação do provedor |
| Cartão | `APROVADA` ou `RECUSADA`, já decidida | na própria chamada |

Cliente que trate as três igual vai errar: para Pix e boleto, `PENDENTE` é a resposta normal e
não significa falha.

### O número do cartão nunca passa por aqui

O cliente tokeniza no navegador, com o SDK do provedor, e envia **o token**. Este serviço não
recebe, não registra e não encaminha número de cartão — o que mantém o escopo de PCI fora da
nossa infraestrutura.

---

## A situação da cobrança é nossa, não do provedor

Cada cobrança é **uma ordem com um pagamento** no provedor. A situação vem do status **da
ordem**, e o `status_detail` vira o motivo. A tradução é explícita e **estoura em status
desconhecido**:

| Status da ordem | Situação aqui |
|---|---|
| `processed` | `APROVADA` |
| `created`, `action_required` | `PENDENTE` |
| `processing`, `in_review` | `EM_ANALISE` |
| `failed` | `RECUSADA` |
| `canceled`, `expired` | `CANCELADA` |
| `refunded`, `charged_back` | `ESTORNADA` |
| qualquer outro | **erro** |

`canceled` é com um "l" só, na grafia da API de Orders.

Cair em `PENDENTE` por omissão seria pior: um status novo do provedor deixaria a cobrança
pendente para sempre, sem ninguém notar. Preferimos o erro ruidoso.

**Cobrança encerrada só aceita estorno.** Uma recusa que chegue atrasada, depois da aprovação,
não volta o estado — notificação fora de ordem acontece.

## O valor cobrado é conferido contra o orçamento aprovado

`POST /cobrancas` recebe um valor, e **não confia nele**. Antes de falar com o provedor, o
serviço consulta `GET /ordens/{id}` no `service-track-ordens` e exige que o valor seja
exatamente o total do orçamento **aprovado**.

Sem isso, um cliente poderia cobrar R$ 1,00 de uma ordem cujo orçamento é R$ 400,50. Ordem sem
orçamento aprovado não gera cobrança nenhuma.

**O orçamento é dado de outro serviço.** `OrcamentoAprovado` aqui é um *read model* — quem é dono
do orçamento é o `ordens`, e este serviço só lê pela API dele, nunca pelo banco.

---

## O webhook, e por que ele é a parte delicada

A rota do webhook é **pública por necessidade**: quem chama é o provedor, não um usuário. Então a
autenticidade não vem de credencial, vem da assinatura.

A validação segue a documentação do provedor: manifesto
`id:<data.id>;request-id:<x-request-id>;ts:<ts>;`, HMAC-SHA256 com o segredo do webhook,
comparado com o `v1` do cabeçalho `x-signature`.

Quatro detalhes que não se adivinham:

1. **`data.id` vai em minúscula** no manifesto, mesmo quando o provedor o envia em maiúscula.
2. **A comparação é em tempo constante**, porque comparação de string com saída antecipada
   vaza informação sobre o segredo.
3. **O `ts` tem janela de tolerância** (cinco minutos por padrão). Sem isso, uma notificação
   capturada poderia ser reenviada por um atacante para sempre.
4. **Segredo ausente recusa tudo**, em vez de aceitar tudo. Desligar a verificação exige dizer
   `MERCADO_PAGO_VERIFY_SIGNATURE=false`, e o log avisa a cada start.

### O webhook responde 200 quase sempre, de propósito

Quando a assinatura confere, a resposta é `200` — inclusive para notificação repetida, para tipo
que não tratamos e para ordem que não conhecemos. O tipo tratado é `order`; a URL de
notificação é configurada no painel da aplicação no Mercado Pago, porque a API de Orders não
aceita `notification_url` no pedido. O provedor **reenvia enquanto não receber
2xx**, e reenviar o que já foi tratado só gasta chamada.

Assinatura que não confere responde `401`, e aí o reenvio é desejável: se o segredo estiver
errado do nosso lado, queremos a notificação de novo depois de corrigir.

### Reconciliação existe porque a notificação pode nunca chegar

`POST /cobrancas/{id}/reconciliacao` consulta o provedor e aplica o estado que ele reporta.

Duas razões concretas, e a segunda é estrutural aqui:

- o processo pode ter morrido entre criar a cobrança e receber a resposta, deixando a cobrança
  pendente sem identificador externo;
- **a URL do webhook registrada no painel do provedor morre a cada `destroy` do ambiente.** Este é um
  projeto de ambientes efêmeros; contar só com notificação seria contar com o que não sobrevive.

---

## Idempotência nos dois sentidos

| Direção | Mecanismo |
|---|---|
| nós → provedor | cabeçalho `X-Idempotency-Key` = `<cobrancaId>:<meio>`, então reenvio da mesma cobrança não cria duas ordens |
| provedor → nós | tabela `NOTIFICACOES`, com chave `<tipo>:<acao>:<recursoId>` |
| cliente → nós | pedir a mesma cobrança duas vezes devolve a existente, sem falar com o provedor |

Clicar duas vezes em "pagar" não abre duas cobranças. Cobrança **recusada**, porém, permite
tentar de novo — é o caso do cartão com código de segurança errado.

---

## Arquitetura

```
domain/
  cobranca/       Cobranca, MeioDePagamento, SituacaoDaCobranca
  orcamento/      OrcamentoAprovado — read model do que o ordens aprovou
  vo/             CobrancaId, ValorMonetario
application/
  port/in/api     contrato HTTP e DTOs
  port/out        repositório, gateway de pagamento, ordens, INBOX
  handler/        CobrancaCommandHandler e CobrancaQueryHandler
infra/
  adapter/client  Mercado Pago pela API de Orders (Pix, boleto, cartão) e ordens
  adapter/web     controllers, webhook e contrato de erro
  adapter/repository
  entity/
```

**O gateway é porta, não dependência direta.** `PagamentoGatewayPort` fala em termos de cobrança;
`MercadoPagoGatewayAdapter` traduz. Trocar de provedor é escrever outro adaptador, e a regra de
negócio não sabe que o Mercado Pago existe.

Um teste provou que isso importa: a exigência de token para cartão vivia só no adaptador do
Mercado Pago, e um teste com gateway diferente passou aceitando cartão sem token. A regra foi
para o handler.

---

## Configuração

| Variável | Padrão | Para quê |
|---|---|---|
| `MERCADO_PAGO_ACCESS_TOKEN` | vazio | token do provedor. **Sem ele nada é cobrado** |
| `MERCADO_PAGO_WEBHOOK_SECRET` | vazio | segredo da assinatura. Vazio recusa toda notificação |
| `MERCADO_PAGO_VERIFY_SIGNATURE` | `true` | desligar só fora de ambiente compartilhado |
| `MERCADO_PAGO_WEBHOOK_TOLERANCE` | `5m` | janela do `ts` contra replay |
| `URL_SERVICE_ORDER_GET` | `http://localhost:8082/ordens` | base da consulta de orçamento |
| `ST_CHK_DB_URL`, `ST_CHK_DB_USER`, `ST_CHK_DB_PASSWORD` | — | banco próprio. Obrigatórios |

Timeout e retentativa de cada integração são configuráveis por `TIMEOUT_*` e `RETRY_*`
(`ORDER_CREATE`, `ORDER_GET`, `SERVICE_ORDER_GET`).
**`RETRY_ORDER_CREATE` é `0` de propósito:** a mesma integração cria Pix, boleto e cartão, e
retentar uma cobrança de cartão é arriscar cobrar duas vezes — o cabeçalho de idempotência
protege contra isso, não contra erro de lógica nossa. Pix e boleto que falhem na criação são
recuperados pela reconciliação. A consulta (`ORDER_GET`) retenta três vezes.

## O banco

Schema `CHECKOUT` no `st_chk`, em [`db/postgres/01_baseline_st_chk.sql`](db/postgres/01_baseline_st_chk.sql).
`ddl-auto: validate` — o baseline **é** o schema.

| Tabela | Por que existe |
|---|---|
| `COBRANCAS` | estado da cobrança, com trava otimista |
| `NOTIFICACOES` | INBOX das notificações do provedor |

`COBRANCAS` tem **índice único parcial** em `PAGAMENTO_EXTERNO_ID`: a notificação traz o id do
provedor, não o nosso, e sem o índice a busca varreria a tabela — além de permitir dois registros
apontando para o mesmo pagamento.

## Como este serviço sobe

| Ambiente | Banco | Imagem | Exposição |
|---|---|---|---|
| `local` (kind) | `componentes/postgres-efemero` | `servicetrack-checkout:local` | NodePort 30084 |
| `hml` e `prd` | RDS próprio, criado por `infra/terraform` | ECR do ambiente | só pelo DNS do cluster |

**Nenhum gateway alcança este serviço** (`IAC-ADR-035`). Quem o chama é o BFF, de dentro do
cluster, por `http://service-track-checkout.service-track-checkout.svc.cluster.local`. O NodePort
sobrou só no overlay `local`, onde serve para o kind expor a porta ao host.

### Dois segredos, separados de propósito

| Segredo | Vem de | Sem ele |
|---|---|---|
| `service-track-checkout-db` | `/servicetrack/<env>/checkout/db/*` no SSM | a aplicação não sobe |
| `service-track-checkout-mercado-pago` | `/servicetrack/<env>/checkout/mercado-pago/*` no SSM | sobe, mas a cobrança falha no provedor e o webhook recusa toda notificação |

Os dois entram como `secretRef` **opcional** no Deployment e são criados pela esteira **Banco**,
a partir do SSM. O token do provedor não passa por variável do Terraform, que o gravaria no
state (`I-16`).

### O que o smoke da esteira prova

A imagem sobe contra o baseline do Postgres e são conferidas quatro respostas que **não**
dependem do provedor nem do serviço de ordens:

| Requisição | Esperado |
|---|---|
| `GET /cobrancas/{uuid}` inexistente | 404 |
| `POST /cobrancas` com cartão sem token | 422, sem chamar o provedor |
| `POST /webhooks/mercado-pago` sem assinatura | 401 |
| `GET /actuator/prometheus` | 200 |

Os quatro foram medidos localmente antes de entrarem na esteira.

### Dependência de saída

`URL_SERVICE_ORDER_GET` aponta para o serviço de ordens pelo DNS do cluster. **Ordens
inacessível responde 502**, não 500: a falha é da dependência, não deste serviço.

## Cobertura

Portão em linha 80%, instrução 80%, ramo 60%. Medido em 11/11/2026, com 92 testes:

| Métrica | Atual | Mínimo |
|---|---|---|
| Linha | 88,9% | 80% |
| Instrução | 84,5% | 80% |
| Ramo | 72,0% | 60% |

```bash
cd software && ./gradlew check
```

Os 11 testes da assinatura do webhook cobrem credencial de outro segredo, corpo trocado,
identificador de requisição trocado, notificação velha e segredo ausente — ou seja, as formas de
falsificar uma notificação, não só o caminho felizo.

---

## O que ainda não existe

- **Integração com a saga.** O passo `COBRANCA` está declarado e vazio no `GLOBAL-ADR-010`, e
  depende da `GLOBAL-RFC-011` fechar. Hoje a cobrança é disparada por HTTP, não por mensagem.
- **Dockerfile, `k8s/`, `infra/terraform` e as esteiras.**
- **Teste automatizado contra o provedor real.** Os testes usam dublê. A integração foi
  validada à mão em 11/11/2026, com a conta de teste vendedora: Pix e boleto criados,
  reconciliação de `PENDENTE` a `CANCELADA`. A collection
  `docs/postman/mercado-pago.postman_collection.json` repete essas chamadas sem subir o serviço.
- **Estorno e cancelamento pela API.** Na API de Orders são `POST /v1/orders/{id}/cancel` e
  `POST /v1/orders/{id}/refund`; falta o cliente, a rota e a regra de quem pode pedir.

## Fronteiras

**É dono de:** a cobrança, sua situação, a conversa com o provedor de pagamento, e o próprio banco.

**Não é dono e não altera:** estado da ordem de serviço, orçamento, estoque, usuário, veículo.

Precisou de dado alheio — como o total do orçamento — chama a API do dono. **Nunca o banco.**

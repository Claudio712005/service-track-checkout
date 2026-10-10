# service-track-checkout

Microsserviço de **cobrança** da plataforma ServiceTrack. Cobra uma ordem de serviço por **Pix,
boleto ou cartão** através do Mercado Pago, e é o único lugar do sistema que conversa com um
provedor de pagamento.

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

O provedor tem nove status; nós temos seis situações. A tradução é explícita e **estoura em
status desconhecido**:

| Status do provedor | Situação aqui |
|---|---|
| `approved`, `authorized` | `APROVADA` |
| `pending` | `PENDENTE` |
| `in_process`, `in_mediation` | `EM_ANALISE` |
| `rejected` | `RECUSADA` |
| `cancelled` | `CANCELADA` |
| `refunded`, `charged_back` | `ESTORNADA` |
| qualquer outro | **erro** |

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
que não tratamos e para pagamento que não conhecemos. O provedor **reenvia enquanto não receber
2xx**, e reenviar o que já foi tratado só gasta chamada.

Assinatura que não confere responde `401`, e aí o reenvio é desejável: se o segredo estiver
errado do nosso lado, queremos a notificação de novo depois de corrigir.

### Reconciliação existe porque a notificação pode nunca chegar

`POST /cobrancas/{id}/reconciliacao` consulta o provedor e aplica o estado que ele reporta.

Duas razões concretas, e a segunda é estrutural aqui:

- o processo pode ter morrido entre criar a cobrança e receber a resposta, deixando a cobrança
  pendente sem identificador externo;
- **a URL do webhook registrada no provedor morre a cada `destroy` do ambiente.** Este é um
  projeto de ambientes efêmeros; contar só com notificação seria contar com o que não sobrevive.

---

## Idempotência nos dois sentidos

| Direção | Mecanismo |
|---|---|
| nós → provedor | cabeçalho `X-Idempotency-Key` = `<cobrancaId>:<meio>`, então reenvio da mesma cobrança não cria dois pagamentos |
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
  adapter/client  Mercado Pago (Pix, boleto, cartão) e ordens
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
| `MERCADO_PAGO_NOTIFICATION_URL` | vazio | URL que o provedor chama. **Muda a cada recriação do ambiente** |
| `MERCADO_PAGO_VERIFY_SIGNATURE` | `true` | desligar só fora de ambiente compartilhado |
| `MERCADO_PAGO_WEBHOOK_TOLERANCE` | `5m` | janela do `ts` contra replay |
| `URL_CONSULTAR_ORDEM_SERVICO` | `http://localhost:8082/ordens` | base da consulta de orçamento |
| `ST_CHK_DB_URL`, `ST_CHK_DB_USER`, `ST_CHK_DB_PASSWORD` | — | banco próprio. Obrigatórios |

Timeout e retentativa de cada integração são configuráveis por `TIMEOUT_*` e `RETRY_*`.
**`RETRY_ENVIAR_PAGAMENTO_CARTAO` é `0` de propósito:** retentar uma cobrança de cartão é
arriscar cobrar duas vezes, e o cabeçalho de idempotência protege contra isso, não contra erro
de lógica nossa.

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

## Cobertura

Portão em linha 80%, instrução 80%, ramo 60%. Medido em 09/10/2026, com 78 testes:

| Métrica | Atual | Mínimo |
|---|---|---|
| Linha | 82,0% | 80% |
| Instrução | 81,0% | 80% |
| Ramo | 61,8% | 60% |

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
- **Nenhuma chamada real ao provedor.** Não há credencial de sandbox configurada: todos os
  testes usam dublê.
- **Estorno e cancelamento pela API.** O cliente do provedor já os implementa; falta a rota e a
  regra de quem pode pedir.

## Fronteiras

**É dono de:** a cobrança, sua situação, a conversa com o provedor de pagamento, e o próprio banco.

**Não é dono e não altera:** estado da ordem de serviço, orçamento, estoque, usuário, veículo.

Precisou de dado alheio — como o total do orçamento — chama a API do dono. **Nunca o banco.**

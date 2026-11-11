# CHK-ADR-001: cobrança pela API de Orders do Mercado Pago

## Data
11/11/2026

## Status
Aceita.

---

## Contexto

O `checkout` cobrava pela **Payments API** (`POST /v1/payments`, consulta em
`GET /v1/payments/{id}`). Em `hml`, toda cobrança respondia:

```
401 UNAUTHORIZED — cause 7: Unauthorized use of live credentials
```

A investigação foi feita direto na API do Mercado Pago, sem o `checkout` no meio, com o access
token da aplicação criada **dentro da conta de teste vendedora**:

| Chamada | Resultado |
|---|---|
| `GET /users/me` | conta de teste (`nickname` `TESTUSER…`, tag `test_user`) — o token é o certo |
| `POST /checkout/preferences` (Checkout Pro) | criada |
| `POST /v1/payments` — Pix e boleto | **401, causa 7**, também depois de ativar as credenciais de produção da aplicação |
| `POST /v1/orders` — Pix | criada, `action_required / waiting_transfer`, com QR code |
| `POST /v1/orders` — boleto | criada, `action_required / waiting_payment`, com linha digitável |
| `GET /v1/orders/{id}` | devolve o status e o `external_reference` |
| `POST /v1/orders/{id}/cancel` | ordem `canceled / canceled` |

Foram descartadas, uma a uma, as outras explicações: token da conta real, pod com o token
antigo, chave de idempotência repetida, e e-mail de pagador que não é de teste.

**Em contas novas do Mercado Pago, a credencial de teste do Checkout Transparente só é aceita
pela API de Orders.** A Payments API é a geração anterior; a Orders API é o padrão novo do
Checkout Transparente.

## Decisão

O adaptador do Mercado Pago passa a usar a **API de Orders**:

| Operação | Antes | Agora |
|---|---|---|
| Criar cobrança (Pix, boleto, cartão) | `POST /v1/payments` | `POST /v1/orders`, `type: online`, `processing_mode: automatic` |
| Reconciliar | `GET /v1/payments/{id}` | `GET /v1/orders/{id}` |
| Notificação | tópico `payment` | tópico `order` |

Cada cobrança vira **uma ordem com um pagamento** (`transactions.payments[0]`). O
`external_reference` da ordem é o id da cobrança, como era no pagamento.

### Diferenças da Orders API que o código precisa respeitar

| Diferença | Consequência |
|---|---|
| **Identificador alfanumérico** (`ORD01…`, `ORDTST01…` em teste) | `PAGAMENTO_EXTERNO_ID` deixa de ser `BIGINT` e vira `VARCHAR`; o domínio e a porta passam de `Long` a `String` |
| **Valores como texto** (`"total_amount": "149.90"`) | o adaptador formata com duas casas, sem notação científica |
| **Schema estrito** — campo desconhecido responde `400 unsupported_properties` | o payload leva só campos aceitos. **`notification_url` não é aceito**: a notificação é configurada no painel da aplicação, e a propriedade `mercado-pago.notification-url` sai |
| **Status em dois níveis** — da ordem e de cada pagamento | a situação da cobrança vem do status **da ordem**; o `status_detail` vira o motivo |
| **`canceled` com um "l"** | o mapeamento de status usa a grafia da Orders API |

### Mapeamento de status

| Status da ordem | Situação da cobrança |
|---|---|
| `processed` | `APROVADA` |
| `created`, `action_required` | `PENDENTE` |
| `processing`, `in_review` | `EM_ANALISE` |
| `failed` | `RECUSADA` |
| `canceled`, `expired` | `CANCELADA` |
| `refunded`, `charged_back` | `ESTORNADA` |

## Consequências

- **A cobrança volta a funcionar com credencial de teste**, que é a única que um ambiente de
  apresentação pode usar.
- **Contrato do próprio `checkout` muda num campo:** `pagamentoExternoId` em
  `GET /cobrancas/{id}` passa de número a texto. Nenhum serviço consome o `checkout` hoje — o BFF
  não tem rota para ele —, então a mudança não quebra ninguém.
- **Baseline do banco editado**, não migrado: o ambiente é efêmero e o baseline é reaplicado a
  cada recriação (regra 11 do workspace).
- **Cancelamento e estorno pela Payments API saem do cliente.** Nenhum código de produção os
  chamava. Se voltarem a ser necessários, são `POST /v1/orders/{id}/cancel` e
  `POST /v1/orders/{id}/refund`.
- **O emissor do cartão (`issuer_id`) deixa de ser enviado.** A Orders API identifica o emissor
  pelo token do cartão.
- **A collection `docs/postman/mercado-pago.postman_collection.json`** reproduz as duas APIs e
  é a referência para testar a credencial sem subir o serviço.

## Referências

- `workspace/delivery/bugs-hml-2026-11-10.md`, `B-22` — a investigação completa.
- [Mercado Pago — referência da API de Orders](https://www.mercadopago.com.br/developers/pt/reference/orders/online-payments/create/post)

package com.clau.service_track.checkout.infra.adapter.client.mercadopago

object MercadoPagoPayloads {

    const val PIX_CRIADO = """
        {
          "id": 1234,
          "status": "pending",
          "status_detail": "pending_waiting_transfer",
          "payment_method_id": "pix",
          "payment_type_id": "bank_transfer",
          "transaction_amount": 350.90,
          "currency_id": "BRL",
          "external_reference": "os-0001",
          "date_created": "2026-10-08T10:00:00.000-03:00",
          "date_of_expiration": "2026-10-09T10:00:00.000-03:00",
          "point_of_interaction": {
            "type": "PIX",
            "transaction_data": {
              "qr_code": "00020126580014br.gov.bcb.pix",
              "qr_code_base64": "iVBORw0KGgoAAAANS",
              "ticket_url": "https://www.mercadopago.com.br/payments/1234/ticket"
            }
          }
        }
    """

    const val BOLETO_CRIADO = """
        {
          "id": 1235,
          "status": "pending",
          "status_detail": "pending_waiting_payment",
          "payment_method_id": "bolbradesco",
          "payment_type_id": "ticket",
          "transaction_amount": 120.00,
          "currency_id": "BRL",
          "external_reference": "os-0002",
          "transaction_details": {
            "external_resource_url": "https://www.mercadopago.com.br/sandbox/payments/1235/boleto",
            "digitable_line": "23793381286000000000000000000000000000000000",
            "barcode": { "content": "23793381286000000000000000000000000000000000" }
          }
        }
    """

    const val CARTAO_APROVADO = """
        {
          "id": 1236,
          "status": "approved",
          "status_detail": "accredited",
          "payment_method_id": "master",
          "payment_type_id": "credit_card",
          "installments": 3,
          "transaction_amount": 900.00,
          "currency_id": "BRL",
          "external_reference": "os-0003",
          "captured": true,
          "card": {
            "first_six_digits": "503143",
            "last_four_digits": "6351",
            "expiration_month": 11,
            "expiration_year": 2030,
            "cardholder": {
              "name": "APRO",
              "identification": { "type": "CPF", "number": "12345678909" }
            }
          },
          "fee_details": [
            { "type": "mercadopago_fee", "fee_payer": "collector", "amount": 44.10 }
          ]
        }
    """

    const val CANCELADO = """
        {
          "id": 1234,
          "status": "cancelled",
          "status_detail": "by_collector",
          "payment_method_id": "pix",
          "transaction_amount": 350.90
        }
    """

    const val ESTORNO = """
        {
          "id": 55,
          "payment_id": 1236,
          "amount": 900.00,
          "status": "approved",
          "refund_mode": "standard",
          "date_created": "2026-10-08T10:05:00.000-03:00"
        }
    """

    const val ERRO_REQUISICAO = """
        {
          "message": "invalid card_token_id",
          "error": "bad_request",
          "status": 400,
          "cause": [ { "code": "2062", "description": "invalid card_token_id" } ]
        }
    """

    const val ERRO_INDISPONIVEL = """
        { "message": "internal_error", "error": "internal_error", "status": 500 }
    """
}

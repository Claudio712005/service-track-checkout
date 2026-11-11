package com.clau.service_track.checkout.infra.adapter.client.mercadopago

object MercadoPagoPayloads {

    const val ORDEM_PIX_CRIADA = """
        {
          "id": "ORDTST01PIX0000000000000000001",
          "type": "online",
          "processing_mode": "automatic",
          "external_reference": "cobranca-0001",
          "total_amount": "350.90",
          "status": "action_required",
          "status_detail": "waiting_transfer",
          "transactions": {
            "payments": [
              {
                "id": "PAY01PIX000000000000000001",
                "amount": "350.90",
                "status": "action_required",
                "status_detail": "waiting_transfer",
                "payment_method": {
                  "id": "pix",
                  "type": "bank_transfer",
                  "ticket_url": "https://www.mercadopago.com.br/sandbox/payments/1/ticket",
                  "qr_code": "00020126580014br.gov.bcb.pix",
                  "qr_code_base64": "iVBORw0KGgoAAAANS"
                }
              }
            ]
          }
        }
    """

    const val ORDEM_BOLETO_CRIADA = """
        {
          "id": "ORDTST01BOL0000000000000000001",
          "external_reference": "cobranca-0002",
          "total_amount": "350.90",
          "status": "action_required",
          "status_detail": "waiting_payment",
          "transactions": {
            "payments": [
              {
                "id": "PAY01BOL000000000000000001",
                "status": "action_required",
                "status_detail": "waiting_payment",
                "payment_method": {
                  "id": "boleto",
                  "type": "ticket",
                  "ticket_url": "https://www.mercadopago.com.br/payments/2/ticket",
                  "digitable_line": "23793380296060108511566006333307616320000"
                }
              }
            ]
          }
        }
    """

    const val ORDEM_CARTAO_PROCESSADA = """
        {
          "id": "ORDTST01CAR0000000000000000001",
          "external_reference": "cobranca-0003",
          "total_amount": "350.90",
          "status": "processed",
          "status_detail": "accredited",
          "transactions": {
            "payments": [
              {
                "id": "PAY01CAR000000000000000001",
                "status": "processed",
                "status_detail": "accredited",
                "payment_method": { "id": "master", "type": "credit_card" }
              }
            ]
          }
        }
    """

    const val ORDEM_CANCELADA = """
        {
          "id": "ORDTST01PIX0000000000000000001",
          "status": "canceled",
          "status_detail": "canceled",
          "transactions": {
            "payments": [ { "id": "PAY01PIX000000000000000001", "status": "canceled", "status_detail": "canceled_transaction" } ]
          }
        }
    """

    const val ORDEM_SEM_DETALHE = """
        {
          "id": "ORDTST01PIX0000000000000000002",
          "status": "failed",
          "transactions": {
            "payments": [ { "id": "PAY01PIX000000000000000002", "status": "failed", "status_detail": "rejected_by_issuer" } ]
          }
        }
    """

    const val ERRO_REQUISICAO = """
        {
          "errors": [
            {
              "code": "unsupported_properties",
              "message": "Properties not supported",
              "details": [ "additionalProperties 'notification_url' not allowed" ]
            }
          ]
        }
    """

    const val ERRO_CREDENCIAL = """
        {
          "message": "Unauthorized use of live credentials",
          "error": "unauthorized",
          "status": 401,
          "cause": [ { "code": "7", "description": "Unauthorized use of live credentials" } ]
        }
    """

    const val ERRO_INDISPONIVEL = """
        { "message": "internal_error", "error": "internal_error", "status": 500 }
    """
}

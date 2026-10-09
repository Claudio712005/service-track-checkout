package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.PayerInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.PixCheckoutInput
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired

class PixCheckoutTest : MercadoPagoTestBase() {

    @Autowired
    private lateinit var pixCheckout: PixCheckout

    private val input = PixCheckoutInput(
        transactionAmount = BigDecimal("350.90"),
        description = "Ordem de servico 0001",
        externalReference = "os-0001",
        dateOfExpiration = OffsetDateTime.of(2026, 10, 9, 10, 0, 0, 0, ZoneOffset.ofHours(-3)),
        payer = PayerInput(
            email = "cliente@exemplo.com",
            firstName = "Clara",
            lastName = "Souza",
            documentType = "CPF",
            documentNumber = "12345678909",
            areaCode = "11",
            phoneNumber = "999998888",
        ),
    )

    @Test
    fun `envia pagamento pix no formato do checkout transparente`() {
        stub.enqueue(201, MercadoPagoPayloads.PIX_CRIADO)

        pixCheckout.send(input, "os-0001:PAGAMENTO:1")

        val corpo = corpoEnviado()
        assertEquals("pix", corpo.path("payment_method_id").asString())
        assertEquals(0, corpo.path("transaction_amount").decimalValue().compareTo(BigDecimal("350.90")))
        assertEquals("os-0001", corpo.path("external_reference").asString())
        assertEquals("2026-10-09T10:00:00-03:00", corpo.path("date_of_expiration").asString())
        assertEquals("cliente@exemplo.com", corpo.path("payer").path("email").asString())
        assertEquals("Clara", corpo.path("payer").path("first_name").asString())
        assertEquals("CPF", corpo.path("payer").path("identification").path("type").asString())
        assertEquals("12345678909", corpo.path("payer").path("identification").path("number").asString())
        assertEquals("11", corpo.path("payer").path("phone").path("area_code").asString())
    }

    @Test
    fun `nao envia campos nulos nem campos de cartao`() {
        stub.enqueue(201, MercadoPagoPayloads.PIX_CRIADO)

        pixCheckout.send(input, "os-0001:PAGAMENTO:1")

        val corpo = corpoEnviado()
        assertFalse(corpo.has("token"))
        assertFalse(corpo.has("installments"))
        assertFalse(corpo.has("issuer_id"))
        assertFalse(corpo.path("payer").has("address"))
    }

    @Test
    fun `usa a url de notificacao padrao e envia chave de idempotencia`() {
        stub.enqueue(201, MercadoPagoPayloads.PIX_CRIADO)

        pixCheckout.send(input, "os-0001:PAGAMENTO:1")

        assertEquals(
            "https://exemplo/webhooks/mercado-pago",
            corpoEnviado().path("notification_url").asString(),
        )
        assertEquals("os-0001:PAGAMENTO:1", stub.lastRequest.headers.getFirst("X-Idempotency-Key"))
        assertEquals("Bearer access-token-de-teste", stub.lastRequest.headers.getFirst("Authorization"))
        assertEquals("POST", stub.lastRequest.method)
    }

    @Test
    fun `le o qr code da resposta`() {
        stub.enqueue(201, MercadoPagoPayloads.PIX_CRIADO)

        val resposta = pixCheckout.send(input, "os-0001:PAGAMENTO:1")

        assertEquals(1234L, resposta.id)
        assertEquals("pending", resposta.status)
        assertEquals("pending_waiting_transfer", resposta.statusDetail)
        val dadosDaTransacao = resposta.pointOfInteraction?.transactionData
        assertEquals("00020126580014br.gov.bcb.pix", dadosDaTransacao?.qrCode)
        assertEquals("iVBORw0KGgoAAAANS", dadosDaTransacao?.qrCodeBase64)
        assertTrue(dadosDaTransacao?.ticketUrl!!.endsWith("/ticket"))
    }
}

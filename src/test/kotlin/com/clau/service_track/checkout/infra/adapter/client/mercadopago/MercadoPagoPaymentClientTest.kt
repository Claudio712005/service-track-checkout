package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired

class MercadoPagoPaymentClientTest : MercadoPagoTestBase() {

    @Autowired
    private lateinit var paymentClient: MercadoPagoPaymentClient

    @Test
    fun `consulta pagamento pelo identificador`() {
        stub.enqueue(200, MercadoPagoPayloads.PIX_CRIADO)

        val resposta = paymentClient.find(1234)

        assertEquals(1234L, resposta.id)
        assertEquals("GET", stub.lastRequest.method)
        assertEquals("/v1/payments/1234", stub.lastRequest.path)
    }

    @Test
    fun `cancela pagamento enviando status cancelado`() {
        stub.enqueue(200, MercadoPagoPayloads.CANCELADO)

        val resposta = paymentClient.cancel(1234, "os-0001:CANCELAMENTO:1")

        assertEquals("cancelled", resposta.status)
        assertEquals("PUT", stub.lastRequest.method)
        assertEquals("/v1/payments/1234", stub.lastRequest.path)
        assertEquals("cancelled", corpoEnviado().path("status").asString())
    }

    @Test
    fun `estorna pagamento integral sem enviar valor`() {
        stub.enqueue(201, MercadoPagoPayloads.ESTORNO)

        val resposta = paymentClient.refund(1236, null, "os-0003:ESTORNO:1")

        assertEquals(55L, resposta.id)
        assertEquals(1236L, resposta.paymentId)
        assertEquals("/v1/payments/1236/refunds", stub.lastRequest.path)
        assertEquals("{}", stub.lastRequest.body)
    }

    @Test
    fun `estorna pagamento parcial enviando valor`() {
        stub.enqueue(201, MercadoPagoPayloads.ESTORNO)

        paymentClient.refund(1236, BigDecimal("100.00"), "os-0003:ESTORNO:1")

        assertEquals(
            0,
            corpoEnviado().path("amount").decimalValue().compareTo(BigDecimal("100.00")),
        )
    }

    @Test
    fun `traduz indisponibilidade do mercado pago em excecao retentavel`() {
        stub.enqueue(500, MercadoPagoPayloads.ERRO_INDISPONIVEL)

        val excecao = runCatching { paymentClient.find(1234) }.exceptionOrNull()

        assertTrue(excecao is MercadoPagoUnavailableException)
        assertEquals(500, excecao.statusCode.value())
        assertEquals("internal_error", excecao.error?.error)
    }

    @Test
    fun `aceita resposta de erro sem corpo json`() {
        stub.enqueue(503, "<html>indisponivel</html>")

        val excecao = runCatching { paymentClient.find(1234) }.exceptionOrNull()

        assertTrue(excecao is MercadoPagoUnavailableException)
        assertEquals(null, excecao.error)
    }
}

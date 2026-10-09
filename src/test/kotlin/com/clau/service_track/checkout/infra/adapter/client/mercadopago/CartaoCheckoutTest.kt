package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.CartaoCheckoutInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.PayerInput
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired

class CartaoCheckoutTest : MercadoPagoTestBase() {

    @Autowired
    private lateinit var cartaoCheckout: CartaoCheckout

    private val input = CartaoCheckoutInput(
        transactionAmount = BigDecimal("900.00"),
        description = "Ordem de servico 0003",
        externalReference = "os-0003",
        token = "token-do-cartao",
        paymentMethodId = "master",
        installments = 3,
        issuerId = "24",
        statementDescriptor = "SERVICETRACK",
        threeDSecureMode = "optional",
        payer = PayerInput(
            email = "cliente@exemplo.com",
            firstName = "Clara",
            lastName = "Souza",
            documentType = "CPF",
            documentNumber = "12345678909",
        ),
    )

    @Test
    fun `envia pagamento por cartao com token e parcelas`() {
        stub.enqueue(201, MercadoPagoPayloads.CARTAO_APROVADO)

        cartaoCheckout.send(input, "os-0003:PAGAMENTO:1")

        val corpo = corpoEnviado()
        assertEquals("master", corpo.path("payment_method_id").asString())
        assertEquals("token-do-cartao", corpo.path("token").asString())
        assertEquals(3, corpo.path("installments").asInt())
        assertEquals("24", corpo.path("issuer_id").asString())
        assertEquals("SERVICETRACK", corpo.path("statement_descriptor").asString())
        assertEquals("optional", corpo.path("three_d_secure_mode").asString())
        assertTrue(corpo.path("capture").asBoolean())
    }

    @Test
    fun `le o cartao e as tarifas da resposta`() {
        stub.enqueue(201, MercadoPagoPayloads.CARTAO_APROVADO)

        val resposta = cartaoCheckout.send(input, "os-0003:PAGAMENTO:1")

        assertEquals("approved", resposta.status)
        assertEquals("accredited", resposta.statusDetail)
        assertEquals(3, resposta.installments)
        assertEquals("503143", resposta.card?.firstSixDigits)
        assertEquals("6351", resposta.card?.lastFourDigits)
        assertEquals(11, resposta.card?.expirationMonth)
        assertEquals("APRO", resposta.card?.cardholder?.name)
        assertEquals("CPF", resposta.card?.cardholder?.identification?.type)
        assertEquals(1, resposta.feeDetails?.size)
        assertEquals("mercadopago_fee", resposta.feeDetails?.first()?.type)
    }

    @Test
    fun `traduz recusa do mercado pago em excecao de requisicao`() {
        stub.enqueue(400, MercadoPagoPayloads.ERRO_REQUISICAO)

        val excecao = runCatching { cartaoCheckout.send(input, "os-0003:PAGAMENTO:1") }
            .exceptionOrNull()

        assertTrue(excecao is MercadoPagoRequestException)
        assertEquals(400, excecao.statusCode.value())
        assertEquals("2062", excecao.error?.cause?.first()?.code)
        assertTrue(excecao.message!!.contains("2062"))
    }
}

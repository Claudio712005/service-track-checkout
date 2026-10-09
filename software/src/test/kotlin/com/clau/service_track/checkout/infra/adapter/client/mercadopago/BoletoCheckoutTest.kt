package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.BoletoCheckoutInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.PayerAddressInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.PayerInput
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.springframework.beans.factory.annotation.Autowired

class BoletoCheckoutTest : MercadoPagoTestBase() {

    @Autowired
    private lateinit var boletoCheckout: BoletoCheckout

    private val input = BoletoCheckoutInput(
        transactionAmount = BigDecimal("120.00"),
        description = "Ordem de servico 0002",
        externalReference = "os-0002",
        payer = PayerInput(
            email = "cliente@exemplo.com",
            firstName = "Clara",
            lastName = "Souza",
            documentType = "CPF",
            documentNumber = "12345678909",
        ),
        address = PayerAddressInput(
            zipCode = "06233200",
            streetName = "Av Ipiranga",
            streetNumber = "1500",
            neighborhood = "Centro",
            city = "Sao Paulo",
            federalUnit = "SP",
        ),
    )

    @Test
    fun `envia pagamento por boleto com endereco do pagador`() {
        stub.enqueue(201, MercadoPagoPayloads.BOLETO_CRIADO)

        boletoCheckout.send(input, "os-0002:PAGAMENTO:1")

        val corpo = corpoEnviado()
        assertEquals("bolbradesco", corpo.path("payment_method_id").asString())
        val endereco = corpo.path("payer").path("address")
        assertEquals("06233200", endereco.path("zip_code").asString())
        assertEquals("Av Ipiranga", endereco.path("street_name").asString())
        assertEquals("1500", endereco.path("street_number").asString())
        assertEquals("Centro", endereco.path("neighborhood").asString())
        assertEquals("Sao Paulo", endereco.path("city").asString())
        assertEquals("SP", endereco.path("federal_unit").asString())
    }

    @Test
    fun `omite telefone quando o pagador nao informa`() {
        stub.enqueue(201, MercadoPagoPayloads.BOLETO_CRIADO)

        boletoCheckout.send(input, "os-0002:PAGAMENTO:1")

        assertFalse(corpoEnviado().path("payer").has("phone"))
    }

    @Test
    fun `le a linha digitavel e o codigo de barras da resposta`() {
        stub.enqueue(201, MercadoPagoPayloads.BOLETO_CRIADO)

        val resposta = boletoCheckout.send(input, "os-0002:PAGAMENTO:1")

        assertEquals(1235L, resposta.id)
        assertEquals("ticket", resposta.paymentTypeId)
        val detalhes = resposta.transactionDetails
        assertEquals("23793381286000000000000000000000000000000000", detalhes?.digitableLine)
        assertEquals("23793381286000000000000000000000000000000000", detalhes?.barcode?.content)
        assertEquals(
            "https://www.mercadopago.com.br/sandbox/payments/1235/boleto",
            detalhes?.externalResourceUrl,
        )
    }
}

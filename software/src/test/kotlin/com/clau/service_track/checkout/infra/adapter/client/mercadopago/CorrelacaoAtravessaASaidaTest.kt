package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.infra.adapter.web.filter.CorrelationFilter
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired

class CorrelacaoAtravessaASaidaTest : MercadoPagoTestBase() {

    @Autowired
    private lateinit var paymentClient: MercadoPagoPaymentClient

    @AfterTest
    fun limparMdc() = MDC.clear()

    private fun cabecalhoEnviado(nome: String): String? =
        stub.lastRequest.headers.getFirst(nome)

    @Test
    fun `a correlacao do MDC viaja no cabecalho da chamada de saida`() {
        MDC.put(CorrelationFilter.CORRELATION_FIELD, "correlacao-da-requisicao")
        MDC.put(CorrelationFilter.REQUEST_FIELD, "requisicao-unica")
        stub.enqueue(200, MercadoPagoPayloads.PIX_CRIADO)

        paymentClient.find(1234)

        assertEquals("correlacao-da-requisicao", cabecalhoEnviado(CorrelationFilter.CORRELATION_HEADER))
        assertEquals("requisicao-unica", cabecalhoEnviado(CorrelationFilter.REQUEST_HEADER))
    }

    @Test
    fun `sem correlacao no MDC a chamada sai sem o cabecalho, em vez de sair vazia`() {
        stub.enqueue(200, MercadoPagoPayloads.PIX_CRIADO)

        paymentClient.find(1234)

        assertNull(cabecalhoEnviado(CorrelationFilter.CORRELATION_HEADER))
        assertNull(cabecalhoEnviado(CorrelationFilter.REQUEST_HEADER))
    }

    @Test
    fun `correlacao em branco no MDC nao vira cabecalho`() {
        MDC.put(CorrelationFilter.CORRELATION_FIELD, "   ")
        stub.enqueue(200, MercadoPagoPayloads.PIX_CRIADO)

        paymentClient.find(1234)

        assertNull(cabecalhoEnviado(CorrelationFilter.CORRELATION_HEADER))
    }
}

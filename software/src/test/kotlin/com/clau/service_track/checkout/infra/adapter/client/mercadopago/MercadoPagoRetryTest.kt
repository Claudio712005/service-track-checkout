package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.infra.config.client.ClientsIntegrations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:st_chk_retry;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.otlp.tracing.export.enabled=false",
        "management.otlp.metrics.export.enabled=false",
    ],
)
class MercadoPagoRetryTest {

    @Autowired
    private lateinit var paymentClient: MercadoPagoPaymentClient

    @BeforeEach
    fun limparStub() {
        stub.reset()
    }

    @Test
    fun `retenta enquanto o mercado pago responde erro de servidor`() {
        repeat(3) { stub.enqueue(500, MercadoPagoPayloads.ERRO_INDISPONIVEL) }

        val excecao = runCatching { paymentClient.find(1234) }.exceptionOrNull()

        assertTrue(excecao is MercadoPagoUnavailableException)
        assertEquals(3, stub.requests.size)
    }

    @Test
    fun `para de retentar quando a tentativa seguinte tem sucesso`() {
        stub.enqueue(500, MercadoPagoPayloads.ERRO_INDISPONIVEL)
        stub.enqueue(200, MercadoPagoPayloads.PIX_CRIADO)

        val resposta = paymentClient.find(1234)

        assertEquals(1234L, resposta.id)
        assertEquals(2, stub.requests.size)
    }

    @Test
    fun `nao retenta recusa do mercado pago`() {
        stub.enqueue(400, MercadoPagoPayloads.ERRO_REQUISICAO)

        val excecao = runCatching { paymentClient.find(1234) }.exceptionOrNull()

        assertTrue(excecao is MercadoPagoRequestException)
        assertEquals(1, stub.requests.size)
    }

    private companion object {

        @JvmStatic
        val stub = MercadoPagoStub()

        @JvmStatic
        @DynamicPropertySource
        fun integracoes(registry: DynamicPropertyRegistry) {
            ClientsIntegrations.entries.forEach { integration ->
                registry.add("clients.integrations.${integration.name}.url") { stub.baseUrl }
                registry.add("clients.integrations.${integration.name}.timeout") { 2000 }
                registry.add("clients.integrations.${integration.name}.connect-timeout") { 2000 }
                registry.add("clients.integrations.${integration.name}.retry") { 2 }
            }
            registry.add("mercado-pago.access-token") { "access-token-de-teste" }
        }
    }
}

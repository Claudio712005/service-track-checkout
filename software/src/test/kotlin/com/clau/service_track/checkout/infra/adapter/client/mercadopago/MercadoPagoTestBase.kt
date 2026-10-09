package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.infra.config.client.ClientsIntegrations
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:st_chk;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.otlp.tracing.export.enabled=false",
        "management.otlp.metrics.export.enabled=false",
    ],
)
abstract class MercadoPagoTestBase {

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun limparStub() {
        stub.reset()
    }

    protected fun corpoEnviado(): JsonNode = objectMapper.readTree(stub.lastRequest.body)

    protected companion object {

        @JvmStatic
        val stub = MercadoPagoStub()

        @JvmStatic
        @DynamicPropertySource
        fun integracoes(registry: DynamicPropertyRegistry) {
            ClientsIntegrations.entries.forEach { integration ->
                registry.add("clients.integrations.${integration.name}.url") { stub.baseUrl }
                registry.add("clients.integrations.${integration.name}.timeout") { 2000 }
                registry.add("clients.integrations.${integration.name}.connect-timeout") { 2000 }
                registry.add("clients.integrations.${integration.name}.retry") { 0 }
            }
            registry.add("mercado-pago.access-token") { "access-token-de-teste" }
            registry.add("mercado-pago.notification-url") { "https://exemplo/webhooks/mercado-pago" }
        }
    }
}

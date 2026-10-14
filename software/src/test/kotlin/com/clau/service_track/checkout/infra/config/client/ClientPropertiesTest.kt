package com.clau.service_track.checkout.infra.config.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientPropertiesTest {

    @Test
    fun `devolve a configuracao do client declarado`() {
        val settings = ClientDto(url = "https://api/v1/payments", timeout = 1000, retry = 1)
        val properties = ClientProperties(mapOf(ClientsIntegrations.PAYMENT_GET to settings))

        assertEquals(settings, properties.getClient(ClientsIntegrations.PAYMENT_GET))
    }

    @Test
    fun `falha quando o client nao tem configuracao`() {
        val properties = ClientProperties()

        val excecao = runCatching { properties.getClient(ClientsIntegrations.PAYMENT_REFUND) }
            .exceptionOrNull()

        assertTrue(excecao is IllegalStateException)
        assertEquals("client sem configuracao: PAYMENT_REFUND", excecao.message)
    }
}

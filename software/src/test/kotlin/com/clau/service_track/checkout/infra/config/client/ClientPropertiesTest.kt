package com.clau.service_track.checkout.infra.config.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientPropertiesTest {

    @Test
    fun `devolve a configuracao do client declarado`() {
        val settings = ClientDto(url = "https://api/v1/orders", timeout = 1000, retry = 1)
        val properties = ClientProperties(mapOf(ClientsIntegrations.ORDER_GET to settings))

        assertEquals(settings, properties.getClient(ClientsIntegrations.ORDER_GET))
    }

    @Test
    fun `falha quando o client nao tem configuracao`() {
        val properties = ClientProperties()

        val excecao = runCatching { properties.getClient(ClientsIntegrations.ORDER_CREATE) }
            .exceptionOrNull()

        assertTrue(excecao is IllegalStateException)
        assertEquals("client sem configuracao: ORDER_CREATE", excecao.message)
    }
}

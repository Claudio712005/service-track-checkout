package com.clau.service_track.checkout.infra.config.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientPropertiesTest {

    @Test
    fun `devolve a configuracao do client declarado`() {
        val settings = ClientDto(url = "https://api/v1/payments", timeout = 1000, retry = 1)
        val properties = ClientProperties(mapOf(ClientsIntegrations.CONSULTAR_PAGAMENTO to settings))

        assertEquals(settings, properties.getClient(ClientsIntegrations.CONSULTAR_PAGAMENTO))
    }

    @Test
    fun `falha quando o client nao tem configuracao`() {
        val properties = ClientProperties()

        val excecao = runCatching { properties.getClient(ClientsIntegrations.ESTORNAR_PAGAMENTO) }
            .exceptionOrNull()

        assertTrue(excecao is IllegalStateException)
        assertEquals("client sem configuracao: ESTORNAR_PAGAMENTO", excecao.message)
    }
}

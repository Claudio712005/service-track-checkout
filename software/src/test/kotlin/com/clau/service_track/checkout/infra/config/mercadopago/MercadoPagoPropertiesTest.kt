package com.clau.service_track.checkout.infra.config.mercadopago

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MercadoPagoPropertiesTest {

    @Test
    fun `url de notificacao vazia e tratada como ausente`() {
        assertNull(MercadoPagoProperties(notificationUrl = "").notificationUrl)
    }

    @Test
    fun `url de notificacao so com espacos e tratada como ausente`() {
        assertNull(MercadoPagoProperties(notificationUrl = "   ").notificationUrl)
    }

    @Test
    fun `url de notificacao preenchida e mantida`() {
        assertEquals(
            "https://exemplo/webhooks/mercado-pago",
            MercadoPagoProperties(notificationUrl = "https://exemplo/webhooks/mercado-pago").notificationUrl,
        )
    }
}

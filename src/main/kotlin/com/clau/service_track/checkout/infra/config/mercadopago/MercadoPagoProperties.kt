package com.clau.service_track.checkout.infra.config.mercadopago

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mercado-pago")
class MercadoPagoProperties(
    val accessToken: String = "",
    val notificationUrl: String? = null,
)

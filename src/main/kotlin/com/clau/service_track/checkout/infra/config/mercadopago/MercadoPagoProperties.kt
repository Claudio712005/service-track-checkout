package com.clau.service_track.checkout.infra.config.mercadopago

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mercado-pago")
class MercadoPagoProperties(
    val accessToken: String = "",
    val notificationUrl: String? = null,
    val webhookSecret: String = "",
    val webhookToleranceWindow: Duration = Duration.ofMinutes(5),
    val verifyWebhookSignature: Boolean = true,
)

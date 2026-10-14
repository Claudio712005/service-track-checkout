package com.clau.service_track.checkout.infra.adapter.web.webhook

import com.clau.service_track.checkout.infra.config.mercadopago.MercadoPagoProperties
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SignatureVerifier(
    private val properties: MercadoPagoProperties,
) {

    private val log = LoggerFactory.getLogger(SignatureVerifier::class.java)

    fun requireValid(signatureHeader: String?, requestId: String?, resourceId: String?) {
        if (!properties.verifyWebhookSignature) {
            log.warn("verificacao de assinatura do webhook DESLIGADA: toda notificacao sera aceita")
            return
        }

        if (properties.webhookSecret.isBlank()) {
            throw InvalidSignatureException(
                "Segredo do webhook nao configurado. Defina mercado-pago.webhook-secret ou desligue a " +
                    "verificacao com mercado-pago.verify-webhook-signature=false fora de producao."
            )
        }

        val parts = partsOf(signatureHeader)
        val timestamp = parts["ts"] ?: throw InvalidSignatureException("Cabecalho x-signature sem ts")
        val received = parts["v1"] ?: throw InvalidSignatureException("Cabecalho x-signature sem v1")

        requireRecent(timestamp)

        val manifest = manifest(resourceId, requestId, timestamp)
        val computed = hmac(manifest)

        if (!equal(computed, received)) {
            throw InvalidSignatureException("Assinatura do webhook nao confere com o manifest")
        }
    }

    private fun partsOf(header: String?): Map<String, String> = (header ?: "")
        .split(',')
        .mapNotNull { chunk ->
            val pair = chunk.split('=', limit = 2)
            if (pair.size == 2) pair[0].trim() to pair[1].trim() else null
        }
        .toMap()

    private fun requireRecent(timestamp: String) {
        val sentAt = timestamp.toLongOrNull()
            ?: throw InvalidSignatureException("ts do x-signature nao e um timestamp valido")

        val age = Instant.now().epochSecond - sentAt
        if (kotlin.math.abs(age) > properties.webhookToleranceWindow.seconds) {
            throw InvalidSignatureException(
                "Notificacao fora da janela de tolerancia: ${age}s de difference"
            )
        }
    }

    private fun manifest(resourceId: String?, requestId: String?, timestamp: String): String {
        val construtor = StringBuilder()
        resourceId?.takeIf { it.isNotBlank() }?.let { construtor.append("id:").append(it.lowercase()).append(';') }
        requestId?.takeIf { it.isNotBlank() }?.let { construtor.append("request-id:").append(it).append(';') }
        construtor.append("ts:").append(timestamp).append(';')
        return construtor.toString()
    }

    private fun hmac(manifest: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(properties.webhookSecret.toByteArray(StandardCharsets.UTF_8), ALGORITHM))
        return mac.doFinal(manifest.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun equal(expected: String, received: String): Boolean {
        val a = expected.toByteArray(StandardCharsets.UTF_8)
        val b = received.lowercase().toByteArray(StandardCharsets.UTF_8)
        if (a.size != b.size) return false

        var difference = 0
        for (i in a.indices) {
            difference = difference or (a[i].toInt() xor b[i].toInt())
        }
        return difference == 0
    }

    private companion object {
        const val ALGORITHM = "HmacSHA256"
    }
}

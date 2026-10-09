package com.clau.service_track.checkout.infra.adapter.web.webhook

import com.clau.service_track.checkout.infra.config.mercadopago.MercadoPagoProperties
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class VerificadorDeAssinatura(
    private val propriedades: MercadoPagoProperties,
) {

    private val log = LoggerFactory.getLogger(VerificadorDeAssinatura::class.java)

    fun exigirValida(cabecalhoDeAssinatura: String?, idDaRequisicao: String?, idDoRecurso: String?) {
        if (!propriedades.verifyWebhookSignature) {
            log.warn("verificacao de assinatura do webhook DESLIGADA: toda notificacao sera aceita")
            return
        }

        if (propriedades.webhookSecret.isBlank()) {
            throw AssinaturaInvalidaException(
                "Segredo do webhook nao configurado. Defina mercado-pago.webhook-secret ou desligue a " +
                    "verificacao com mercado-pago.verify-webhook-signature=false fora de producao."
            )
        }

        val partes = partesDe(cabecalhoDeAssinatura)
        val instante = partes["ts"] ?: throw AssinaturaInvalidaException("Cabecalho x-signature sem ts")
        val recebida = partes["v1"] ?: throw AssinaturaInvalidaException("Cabecalho x-signature sem v1")

        exigirRecente(instante)

        val manifesto = manifesto(idDoRecurso, idDaRequisicao, instante)
        val calculada = hmac(manifesto)

        if (!iguais(calculada, recebida)) {
            throw AssinaturaInvalidaException("Assinatura do webhook nao confere com o manifesto")
        }
    }

    private fun partesDe(cabecalho: String?): Map<String, String> = (cabecalho ?: "")
        .split(',')
        .mapNotNull { pedaco ->
            val par = pedaco.split('=', limit = 2)
            if (par.size == 2) par[0].trim() to par[1].trim() else null
        }
        .toMap()

    private fun exigirRecente(instante: String) {
        val enviadoEm = instante.toLongOrNull()
            ?: throw AssinaturaInvalidaException("ts do x-signature nao e um instante valido")

        val idade = Instant.now().epochSecond - enviadoEm
        if (kotlin.math.abs(idade) > propriedades.webhookToleranceWindow.seconds) {
            throw AssinaturaInvalidaException(
                "Notificacao fora da janela de tolerancia: ${idade}s de diferenca"
            )
        }
    }

    private fun manifesto(idDoRecurso: String?, idDaRequisicao: String?, instante: String): String {
        val construtor = StringBuilder()
        idDoRecurso?.takeIf { it.isNotBlank() }?.let { construtor.append("id:").append(it.lowercase()).append(';') }
        idDaRequisicao?.takeIf { it.isNotBlank() }?.let { construtor.append("request-id:").append(it).append(';') }
        construtor.append("ts:").append(instante).append(';')
        return construtor.toString()
    }

    private fun hmac(manifesto: String): String {
        val algoritmo = Mac.getInstance(ALGORITMO)
        algoritmo.init(SecretKeySpec(propriedades.webhookSecret.toByteArray(StandardCharsets.UTF_8), ALGORITMO))
        return algoritmo.doFinal(manifesto.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun iguais(esperada: String, recebida: String): Boolean {
        val a = esperada.toByteArray(StandardCharsets.UTF_8)
        val b = recebida.lowercase().toByteArray(StandardCharsets.UTF_8)
        if (a.size != b.size) return false

        var diferenca = 0
        for (i in a.indices) {
            diferenca = diferenca or (a[i].toInt() xor b[i].toInt())
        }
        return diferenca == 0
    }

    private companion object {
        const val ALGORITMO = "HmacSHA256"
    }
}

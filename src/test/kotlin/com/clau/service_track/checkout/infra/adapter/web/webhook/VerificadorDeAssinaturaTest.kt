package com.clau.service_track.checkout.infra.adapter.web.webhook

import com.clau.service_track.checkout.infra.config.mercadopago.MercadoPagoProperties
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertFailsWith

class VerificadorDeAssinaturaTest {

    private val segredo = "segredo-de-teste-do-webhook"

    private fun verificador(
        verificar: Boolean = true,
        chave: String = segredo,
        janela: Duration = Duration.ofMinutes(5),
    ) = VerificadorDeAssinatura(
        MercadoPagoProperties(
            accessToken = "token",
            webhookSecret = chave,
            webhookToleranceWindow = janela,
            verifyWebhookSignature = verificar,
        )
    )

    private fun assinar(
        idDoRecurso: String,
        idDaRequisicao: String,
        instante: Long = Instant.now().epochSecond,
        chave: String = segredo,
    ): String {
        val manifesto = "id:${idDoRecurso.lowercase()};request-id:$idDaRequisicao;ts:$instante;"
        val algoritmo = Mac.getInstance("HmacSHA256")
        algoritmo.init(SecretKeySpec(chave.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val v1 = algoritmo.doFinal(manifesto.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "ts=$instante,v1=$v1"
    }

    @Test
    fun `assinatura valida passa`() {
        val assinatura = assinar("1234567890", "req-1")

        verificador().exigirValida(assinatura, "req-1", "1234567890")
    }

    @Test
    fun `identificador do recurso em maiuscula e normalizado, como a documentacao exige`() {
        val assinatura = assinar("ABC123XYZ", "req-1")

        verificador().exigirValida(assinatura, "req-1", "ABC123XYZ")
    }

    @Test
    fun `assinatura de outro segredo e recusada`() {
        val assinatura = assinar("1234567890", "req-1", chave = "segredo-do-atacante")

        assertFailsWith<AssinaturaInvalidaException> {
            verificador().exigirValida(assinatura, "req-1", "1234567890")
        }
    }

    @Test
    fun `corpo trocado apos a assinatura e recusado`() {
        val assinatura = assinar("1234567890", "req-1")

        assertFailsWith<AssinaturaInvalidaException> {
            verificador().exigirValida(assinatura, "req-1", "9999999999")
        }
    }

    @Test
    fun `identificador de requisicao trocado e recusado`() {
        val assinatura = assinar("1234567890", "req-1")

        assertFailsWith<AssinaturaInvalidaException> {
            verificador().exigirValida(assinatura, "req-outro", "1234567890")
        }
    }

    @Test
    fun `notificacao velha e recusada, para que replay nao funcione`() {
        val antiga = Instant.now().epochSecond - 3600
        val assinatura = assinar("1234567890", "req-1", instante = antiga)

        assertFailsWith<AssinaturaInvalidaException> {
            verificador().exigirValida(assinatura, "req-1", "1234567890")
        }
    }

    @Test
    fun `cabecalho sem ts ou sem v1 e recusado`() {
        assertFailsWith<AssinaturaInvalidaException> {
            verificador().exigirValida("v1=abc", "req-1", "1234567890")
        }
        assertFailsWith<AssinaturaInvalidaException> {
            verificador().exigirValida("ts=123", "req-1", "1234567890")
        }
        assertFailsWith<AssinaturaInvalidaException> {
            verificador().exigirValida(null, "req-1", "1234567890")
        }
    }

    @Test
    fun `ts que nao e numero e recusado`() {
        assertFailsWith<AssinaturaInvalidaException> {
            verificador().exigirValida("ts=agora,v1=abc", "req-1", "1234567890")
        }
    }

    @Test
    fun `segredo ausente recusa tudo, em vez de aceitar tudo`() {
        val assinatura = assinar("1234567890", "req-1")

        assertFailsWith<AssinaturaInvalidaException> {
            verificador(chave = "").exigirValida(assinatura, "req-1", "1234567890")
        }
    }

    @Test
    fun `verificacao desligada aceita, e e a unica forma de aceitar sem assinatura`() {
        verificador(verificar = false).exigirValida(null, null, null)
    }
}

package com.clau.service_track.checkout.infra.adapter.web.webhook

import com.clau.service_track.checkout.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.checkout.application.handler.CobrancaCommandHandler
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "Webhook do provedor",
    description = "Recebe a notificação do Mercado Pago. Rota pública por necessidade: quem chama é o " +
        "provedor, não um usuário, então a autenticidade vem da assinatura e não de credencial."
)
@RestController
@RequestMapping("/webhooks/mercado-pago")
class WebhookController(
    private val cobrancas: CobrancaCommandHandler,
    private val assinatura: VerificadorDeAssinatura,
) {

    private val log = LoggerFactory.getLogger(WebhookController::class.java)

    @Operation(
        summary = "Recebe uma notificação de pagamento",
        description = "Valida a assinatura `x-signature` contra o manifesto `id:<data.id>;request-id:" +
            "<x-request-id>;ts:<ts>;` com HMAC-SHA256, e **sempre responde 200 quando a assinatura confere** " +
            "— inclusive para notificação repetida ou de pagamento que não conhecemos. O provedor reenvia " +
            "enquanto não receber 2xx, e reenviar o que já foi tratado só gasta chamada."
    )
    @PostMapping
    fun receber(
        @RequestHeader(name = "x-signature", required = false) cabecalhoDeAssinatura: String?,
        @RequestHeader(name = "x-request-id", required = false) idDaRequisicao: String?,
        @RequestParam(name = "data.id", required = false) idNaQuery: String?,
        @RequestParam(name = "type", required = false) tipoNaQuery: String?,
        @RequestBody(required = false) corpo: NotificacaoDoProvedor?,
    ): ResponseEntity<Void> {
        val recursoId = corpo?.data?.id ?: idNaQuery
        val tipo = corpo?.type ?: tipoNaQuery

        assinatura.exigirValida(cabecalhoDeAssinatura, idDaRequisicao, recursoId)

        if (tipo == null || recursoId == null) {
            log.warn("notificacao sem tipo ou sem recurso, ignorada tipo={} recurso={}", tipo, recursoId)
            return ResponseEntity.status(HttpStatus.OK).build()
        }

        if (tipo != TIPO_DE_PAGAMENTO) {
            log.info("notificacao de tipo {} ignorada: este servico so trata pagamento", tipo)
            return ResponseEntity.ok().build()
        }

        try {
            cobrancas.aplicarNotificacao(tipo, corpo?.action, recursoId)
        } catch (e: RecursoNaoEncontradoException) {
            log.warn("notificacao de pagamento desconhecido, confirmada sem efeito motivo={}", e.message)
        }

        return ResponseEntity.ok().build()
    }

    private companion object {
        const val TIPO_DE_PAGAMENTO = "payment"
    }
}

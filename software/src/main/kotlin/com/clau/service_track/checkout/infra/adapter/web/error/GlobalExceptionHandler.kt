package com.clau.service_track.checkout.infra.adapter.web.error

import com.clau.service_track.checkout.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.checkout.domain.DomainException
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.MercadoPagoRequestException
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.MercadoPagoUnavailableException
import com.clau.service_track.checkout.infra.adapter.web.webhook.AssinaturaInvalidaException
import jakarta.servlet.http.HttpServletRequest
import java.time.OffsetDateTime
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(RecursoNaoEncontradoException::class)
    fun naoEncontrado(erro: RecursoNaoEncontradoException, requisicao: HttpServletRequest) =
        montar(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", erro.message, requisicao)

    @ExceptionHandler(DomainException::class)
    fun regraDeNegocio(erro: DomainException, requisicao: HttpServletRequest) =
        montar(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_DE_NEGOCIO", erro.message, requisicao)

    @ExceptionHandler(IllegalArgumentException::class)
    fun argumentoInvalido(erro: IllegalArgumentException, requisicao: HttpServletRequest) =
        montar(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", erro.message, requisicao)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun corpoInvalido(erro: MethodArgumentNotValidException, requisicao: HttpServletRequest): ResponseEntity<ErroResponse> {
        val violacoes = erro.bindingResult.fieldErrors.joinToString("; ") {
            "${it.field}: ${it.defaultMessage.orEmpty()}"
        }
        return montar(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", "Corpo invalido: $violacoes", requisicao)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun corpoIlegivel(erro: HttpMessageNotReadableException, requisicao: HttpServletRequest) =
        montar(HttpStatus.BAD_REQUEST, "CORPO_ILEGIVEL", "Corpo nao e um JSON valido", requisicao)

    @ExceptionHandler(AssinaturaInvalidaException::class)
    fun assinaturaInvalida(erro: AssinaturaInvalidaException, requisicao: HttpServletRequest): ResponseEntity<ErroResponse> {
        log.warn("notificacao recusada por assinatura motivo={}", erro.message)
        return montar(HttpStatus.UNAUTHORIZED, "ASSINATURA_INVALIDA", "Assinatura da notificacao invalida", requisicao)
    }

    @ExceptionHandler(MercadoPagoRequestException::class)
    fun provedorRecusou(erro: MercadoPagoRequestException, requisicao: HttpServletRequest): ResponseEntity<ErroResponse> {
        log.warn("provedor recusou a cobranca status={} causa={}", erro.statusCode, erro.message)
        return montar(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "PROVEDOR_RECUSOU",
            "O provedor de pagamento recusou a cobranca",
            requisicao,
        )
    }

    @ExceptionHandler(MercadoPagoUnavailableException::class)
    fun provedorIndisponivel(erro: MercadoPagoUnavailableException, requisicao: HttpServletRequest): ResponseEntity<ErroResponse> {
        log.error("provedor de pagamento indisponivel status={}", erro.statusCode)
        return montar(
            HttpStatus.SERVICE_UNAVAILABLE,
            "PROVEDOR_INDISPONIVEL",
            "Provedor de pagamento indisponivel, tente novamente em instantes",
            requisicao,
        )
    }

    @ExceptionHandler(Exception::class)
    fun naoPrevisto(erro: Exception, requisicao: HttpServletRequest): ResponseEntity<ErroResponse> {
        log.error("falha nao prevista rota={}", requisicao.requestURI, erro)
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Falha nao prevista", requisicao)
    }

    private fun montar(
        status: HttpStatus,
        codigo: String,
        mensagem: String?,
        requisicao: HttpServletRequest,
    ): ResponseEntity<ErroResponse> = ResponseEntity.status(status).body(
        ErroResponse(
            timestamp = OffsetDateTime.now(),
            status = status.value(),
            code = codigo,
            message = mensagem ?: status.reasonPhrase,
            path = requisicao.requestURI,
        )
    )
}

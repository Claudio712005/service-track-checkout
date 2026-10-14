package com.clau.service_track.checkout.infra.adapter.web.error

import com.clau.service_track.checkout.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.checkout.domain.DomainException
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.MercadoPagoRequestException
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.MercadoPagoUnavailableException
import com.clau.service_track.checkout.infra.adapter.web.webhook.InvalidSignatureException
import jakarta.servlet.http.HttpServletRequest
import java.time.OffsetDateTime
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.ResourceAccessException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(RecursoNaoEncontradoException::class)
    fun notFound(error: RecursoNaoEncontradoException, request: HttpServletRequest) =
        build(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", error.message, request)

    @ExceptionHandler(DomainException::class)
    fun businessRule(error: DomainException, request: HttpServletRequest) =
        build(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_DE_NEGOCIO", error.message, request)

    @ExceptionHandler(IllegalArgumentException::class)
    fun invalidArgument(error: IllegalArgumentException, request: HttpServletRequest) =
        build(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", error.message, request)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalidBody(error: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val violations = error.bindingResult.fieldErrors.joinToString("; ") {
            "${it.field}: ${it.defaultMessage.orEmpty()}"
        }
        return build(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", "Corpo invalido: $violations", request)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun unreadableBody(error: HttpMessageNotReadableException, request: HttpServletRequest) =
        build(HttpStatus.BAD_REQUEST, "CORPO_ILEGIVEL", "Corpo nao e um JSON valido", request)

    @ExceptionHandler(InvalidSignatureException::class)
    fun invalidSignature(error: InvalidSignatureException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("notificacao recusada por assinatura motivo={}", error.message)
        return build(HttpStatus.UNAUTHORIZED, "ASSINATURA_INVALIDA", "Assinatura da notificacao invalida", request)
    }

    @ExceptionHandler(MercadoPagoRequestException::class)
    fun providerRejected(error: MercadoPagoRequestException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("provedor recusou a cobranca status={} causa={}", error.statusCode, error.message)
        return build(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "PROVEDOR_RECUSOU",
            "O provedor de pagamento recusou a cobranca",
            request,
        )
    }

    @ExceptionHandler(MercadoPagoUnavailableException::class)
    fun providerUnavailable(error: MercadoPagoUnavailableException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.error("provedor de pagamento indisponivel status={}", error.statusCode)
        return build(
            HttpStatus.SERVICE_UNAVAILABLE,
            "PROVEDOR_INDISPONIVEL",
            "Provedor de pagamento indisponivel, tente novamente em instantes",
            request,
        )
    }

    @ExceptionHandler(ResourceAccessException::class)
    fun dependencyUnreachable(error: ResourceAccessException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.error("dependencia inalcancavel rota={} causa={}", request.requestURI, error.message)
        return build(
            HttpStatus.BAD_GATEWAY,
            "DEPENDENCIA_INDISPONIVEL",
            "Dependencia necessaria para a cobranca esta inalcancavel",
            request,
        )
    }

    @ExceptionHandler(Exception::class)
    fun unexpected(error: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.error("falha nao prevista rota={}", request.requestURI, error)
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Falha nao prevista", request)
    }

    private fun build(
        status: HttpStatus,
        code: String,
        message: String?,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = ResponseEntity.status(status).body(
        ErrorResponse(
            timestamp = OffsetDateTime.now(),
            status = status.value(),
            code = code,
            message = message ?: status.reasonPhrase,
            path = request.requestURI,
        )
    )
}

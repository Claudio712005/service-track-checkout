package com.clau.service_track.checkout.infra.adapter.web.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerMapping

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class CorrelationFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(CorrelationFilter::class.java)

    override fun shouldNotFilterAsyncDispatch(): Boolean = false

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val correlation = sanitize(request.getHeader(CORRELATION_HEADER)) ?: generate()
        val requestId = generate()

        MDC.put(CORRELATION_FIELD, correlation)
        MDC.put(REQUEST_FIELD, requestId)
        response.setHeader(CORRELATION_HEADER, correlation)
        response.setHeader(REQUEST_HEADER, requestId)

        val inicio = System.nanoTime()
        try {
            chain.doFilter(request, response)
        } finally {
            record(request, response, inicio)
            MDC.remove(CORRELATION_FIELD)
            MDC.remove(REQUEST_FIELD)
        }
    }

    private fun record(request: HttpServletRequest, response: HttpServletResponse, inicio: Long) {
        if (skipped(request.requestURI)) return

        val duracao = (System.nanoTime() - inicio) / 1_000_000
        val metodo = request.method
        val rota = routeOf(request)
        val status = response.status

        when {
            status >= ERRO_DO_SERVIDOR ->
                log.error(FORMATO, metodo, rota, status, duracao)
            status >= ERRO_DO_CLIENTE ->
                log.warn(FORMATO, metodo, rota, status, duracao)
            metodo != "GET" ->
                log.info(FORMATO, metodo, rota, status, duracao)
            else ->
                log.debug(FORMATO, metodo, rota, status, duracao)
        }
    }

    private fun skipped(rota: String): Boolean = ROTAS_IGNORADAS.any { rota.startsWith(it) }

    private fun sanitize(bruto: String?): String? = bruto
        ?.trim()
        ?.take(TAMANHO_MAXIMO)
        ?.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        ?.ifBlank { null }

    private fun generate(): String = UUID.randomUUID().toString()

    companion object {

        fun routeOf(request: HttpServletRequest): String =
            request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String
                ?: request.requestURI

        const val CORRELATION_HEADER = "X-Correlation-Id"
        const val REQUEST_HEADER = "X-Request-Id"
        const val CORRELATION_FIELD = "correlationId"
        const val REQUEST_FIELD = "requestId"
        private const val TAMANHO_MAXIMO = 64
        private const val ERRO_DO_CLIENTE = 400
        private const val ERRO_DO_SERVIDOR = 500
        private const val FORMATO = "requisicao concluida metodo={} rota={} status={} duracaoMs={}"
        private val ROTAS_IGNORADAS = listOf("/actuator", "/v3/api-docs", "/swagger-ui")
    }
}

package com.clau.service_track.checkout.infra.adapter.web.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.servlet.HandlerMapping

class CorrelationFilterTest {

    private val filtro = CorrelationFilter()

    @AfterTest
    fun limparMdc() = MDC.clear()

    private fun cadeiaQueLe(destino: MutableMap<String, String?>): FilterChain =
        FilterChain { _, _ ->
            destino[CorrelationFilter.CORRELATION_FIELD] = MDC.get(CorrelationFilter.CORRELATION_FIELD)
            destino[CorrelationFilter.REQUEST_FIELD] = MDC.get(CorrelationFilter.REQUEST_FIELD)
        }

    private fun executar(
        requisicao: HttpServletRequest,
        resposta: HttpServletResponse = MockHttpServletResponse(),
        cadeia: FilterChain = FilterChain { _, _ -> },
    ): HttpServletResponse {
        filtro.doFilter(requisicao, resposta, cadeia)
        return resposta
    }

    @Test
    fun `gera correlacao quando o cliente nao manda cabecalho`() {
        val resposta = executar(MockHttpServletRequest("GET", "/cobrancas"))

        val correlacao = resposta.getHeader(CorrelationFilter.CORRELATION_HEADER)
        assertNotNull(correlacao)
        assertTrue(correlacao.isNotBlank())
        assertNotNull(resposta.getHeader(CorrelationFilter.REQUEST_HEADER))
    }

    @Test
    fun `reaproveita a correlacao recebida e devolve no cabecalho`() {
        val requisicao = MockHttpServletRequest("GET", "/cobrancas")
        requisicao.addHeader(CorrelationFilter.CORRELATION_HEADER, "correlacao-de-fora")

        val resposta = executar(requisicao)

        assertEquals("correlacao-de-fora", resposta.getHeader(CorrelationFilter.CORRELATION_HEADER))
    }

    @Test
    fun `o requestId e sempre novo, mesmo reaproveitando a correlacao`() {
        val primeira = MockHttpServletRequest("GET", "/cobrancas")
        primeira.addHeader(CorrelationFilter.CORRELATION_HEADER, "mesma-correlacao")
        val segunda = MockHttpServletRequest("GET", "/cobrancas")
        segunda.addHeader(CorrelationFilter.CORRELATION_HEADER, "mesma-correlacao")

        val um = executar(primeira).getHeader(CorrelationFilter.REQUEST_HEADER)
        val dois = executar(segunda).getHeader(CorrelationFilter.REQUEST_HEADER)

        assertNotEquals(um, dois)
    }

    @Test
    fun `o MDC esta populado durante a cadeia, que e de onde o log le`() {
        val visto = mutableMapOf<String, String?>()
        val requisicao = MockHttpServletRequest("GET", "/cobrancas")
        requisicao.addHeader(CorrelationFilter.CORRELATION_HEADER, "correlacao-no-mdc")

        executar(requisicao, cadeia = cadeiaQueLe(visto))

        assertEquals("correlacao-no-mdc", visto[CorrelationFilter.CORRELATION_FIELD])
        assertNotNull(visto[CorrelationFilter.REQUEST_FIELD])
    }

    @Test
    fun `limpa o MDC ao terminar, para nao vazar entre requisicoes`() {
        executar(MockHttpServletRequest("GET", "/cobrancas"))

        assertNull(MDC.get(CorrelationFilter.CORRELATION_FIELD))
        assertNull(MDC.get(CorrelationFilter.REQUEST_FIELD))
    }

    @Test
    fun `limpa o MDC mesmo quando a cadeia lanca`() {
        val requisicao = MockHttpServletRequest("GET", "/cobrancas")
        val cadeiaQueFalha = FilterChain { _, _ -> throw IllegalStateException("falha no handler") }

        runCatching { filtro.doFilter(requisicao, MockHttpServletResponse(), cadeiaQueFalha) }

        assertNull(MDC.get(CorrelationFilter.CORRELATION_FIELD))
        assertNull(MDC.get(CorrelationFilter.REQUEST_FIELD))
    }

    @Test
    fun `saneia cabecalho com caractere fora do permitido`() {
        val requisicao = MockHttpServletRequest("GET", "/cobrancas")
        requisicao.addHeader(CorrelationFilter.CORRELATION_HEADER, "abc\r\nInjetado: 1 ../x")

        val resposta = executar(requisicao)

        assertEquals("abcInjetado1x", resposta.getHeader(CorrelationFilter.CORRELATION_HEADER))
    }

    @Test
    fun `cabecalho em branco vira correlacao nova`() {
        val requisicao = MockHttpServletRequest("GET", "/cobrancas")
        requisicao.addHeader(CorrelationFilter.CORRELATION_HEADER, "   ")

        val correlacao = executar(requisicao).getHeader(CorrelationFilter.CORRELATION_HEADER)

        assertNotNull(correlacao)
        assertNotEquals("   ", correlacao)
        assertTrue(correlacao.isNotBlank())
    }

    @Test
    fun `corta a correlacao em 64 caracteres`() {
        val requisicao = MockHttpServletRequest("GET", "/cobrancas")
        requisicao.addHeader(CorrelationFilter.CORRELATION_HEADER, "a".repeat(200))

        val correlacao = executar(requisicao).getHeader(CorrelationFilter.CORRELATION_HEADER)

        assertEquals(64, correlacao?.length)
    }

    @Test
    fun `a rota preferida e o padrao casado, nao a uri concreta`() {
        val requisicao = MockHttpServletRequest("GET", "/cobrancas/123")
        requisicao.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/cobrancas/{id}")

        assertEquals("/cobrancas/{id}", CorrelationFilter.routeOf(requisicao))
    }

    @Test
    fun `sem padrao casado a rota cai na uri`() {
        assertEquals("/cobrancas", CorrelationFilter.routeOf(MockHttpServletRequest("GET", "/cobrancas")))
    }
}

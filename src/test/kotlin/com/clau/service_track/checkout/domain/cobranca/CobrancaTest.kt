package com.clau.service_track.checkout.domain.cobranca

import com.clau.service_track.checkout.domain.DomainException
import com.clau.service_track.checkout.domain.vo.ValorMonetario
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CobrancaTest {

    private val ordem = UUID.randomUUID().toString()

    private fun nova(meio: MeioDePagamento = MeioDePagamento.PIX) =
        Cobranca.solicitar(ordem, meio, ValorMonetario(BigDecimal("267.70")))

    @Test
    fun `cobranca nasce pendente e sem identificador do provedor`() {
        val cobranca = nova()

        assertEquals(SituacaoDaCobranca.PENDENTE, cobranca.situacao)
        assertNull(cobranca.pagamentoExternoId)
        assertNull(cobranca.motivo)
    }

    @Test
    fun `cobranca sem ordem de servico e recusada`() {
        assertFailsWith<DomainException> {
            Cobranca.solicitar("   ", MeioDePagamento.PIX, ValorMonetario(BigDecimal("10.00")))
        }
    }

    @Test
    fun `valor zero ou negativo e recusado`() {
        assertFailsWith<DomainException> { ValorMonetario(BigDecimal.ZERO) }
        assertFailsWith<DomainException> { ValorMonetario(BigDecimal("-1.00")) }
    }

    @Test
    fun `valor com mais de duas casas decimais e recusado, porque centavo nao se divide`() {
        assertFailsWith<DomainException> { ValorMonetario(BigDecimal("10.001")) }
    }

    @Test
    fun `registrar no provedor guarda o identificador e a situacao`() {
        val cobranca = nova()

        cobranca.registrarNoProvedor(1001, SituacaoDaCobranca.APROVADA, "accredited")

        assertEquals(1001, cobranca.pagamentoExternoId)
        assertEquals(SituacaoDaCobranca.APROVADA, cobranca.situacao)
        assertEquals("accredited", cobranca.motivo)
    }

    @Test
    fun `trocar o identificador do provedor e recusado`() {
        val cobranca = nova()
        cobranca.registrarNoProvedor(1001, SituacaoDaCobranca.PENDENTE, "pending")

        assertFailsWith<DomainException> {
            cobranca.registrarNoProvedor(2002, SituacaoDaCobranca.APROVADA, "accredited")
        }
    }

    @Test
    fun `mesma situacao chegando de novo nao e mudanca`() {
        val cobranca = nova()
        cobranca.registrarNoProvedor(1001, SituacaoDaCobranca.PENDENTE, "pending")

        assertFalse(cobranca.atualizarPeloProvedor(SituacaoDaCobranca.PENDENTE, "pending"))
    }

    @Test
    fun `cobranca encerrada so aceita estorno`() {
        val cobranca = nova()
        cobranca.registrarNoProvedor(1001, SituacaoDaCobranca.APROVADA, "accredited")

        assertFalse(cobranca.atualizarPeloProvedor(SituacaoDaCobranca.RECUSADA, "atrasada"))
        assertTrue(cobranca.atualizarPeloProvedor(SituacaoDaCobranca.ESTORNADA, "refunded"))
    }

    @Test
    fun `pix e boleto sao assincronos, cartao e sincrono`() {
        assertFalse(MeioDePagamento.PIX.sincrono)
        assertFalse(MeioDePagamento.BOLETO.sincrono)
        assertTrue(MeioDePagamento.CARTAO.sincrono)
    }

    @Test
    fun `meio de pagamento aceita minuscula e recusa desconhecido`() {
        assertEquals(MeioDePagamento.PIX, MeioDePagamento.de("pix"))
        assertFailsWith<IllegalArgumentException> { MeioDePagamento.de("cheque") }
    }

    @Test
    fun `status do provedor e traduzido, e desconhecido estoura em vez de virar pendente`() {
        assertEquals(SituacaoDaCobranca.APROVADA, SituacaoDaCobranca.doMercadoPago("approved"))
        assertEquals(SituacaoDaCobranca.APROVADA, SituacaoDaCobranca.doMercadoPago("authorized"))
        assertEquals(SituacaoDaCobranca.PENDENTE, SituacaoDaCobranca.doMercadoPago("pending"))
        assertEquals(SituacaoDaCobranca.EM_ANALISE, SituacaoDaCobranca.doMercadoPago("in_process"))
        assertEquals(SituacaoDaCobranca.EM_ANALISE, SituacaoDaCobranca.doMercadoPago("in_mediation"))
        assertEquals(SituacaoDaCobranca.RECUSADA, SituacaoDaCobranca.doMercadoPago("rejected"))
        assertEquals(SituacaoDaCobranca.CANCELADA, SituacaoDaCobranca.doMercadoPago("cancelled"))
        assertEquals(SituacaoDaCobranca.ESTORNADA, SituacaoDaCobranca.doMercadoPago("refunded"))
        assertEquals(SituacaoDaCobranca.ESTORNADA, SituacaoDaCobranca.doMercadoPago("charged_back"))

        assertFailsWith<IllegalArgumentException> { SituacaoDaCobranca.doMercadoPago("algo_novo") }
        assertFailsWith<IllegalArgumentException> { SituacaoDaCobranca.doMercadoPago(null) }
    }

    @Test
    fun `apenas aprovada, recusada, cancelada e estornada sao estados encerrados`() {
        assertEquals(
            setOf(
                SituacaoDaCobranca.APROVADA,
                SituacaoDaCobranca.RECUSADA,
                SituacaoDaCobranca.CANCELADA,
                SituacaoDaCobranca.ESTORNADA,
            ),
            SituacaoDaCobranca.entries.filter { it.encerrada }.toSet(),
        )
    }
}

package com.clau.service_track.checkout.infra.adapter.repository

import com.clau.service_track.checkout.domain.cobranca.Cobranca
import com.clau.service_track.checkout.domain.cobranca.MeioDePagamento
import com.clau.service_track.checkout.domain.cobranca.SituacaoDaCobranca
import com.clau.service_track.checkout.domain.vo.CobrancaId
import com.clau.service_track.checkout.domain.vo.ValorMonetario
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CobrancaRepositoryPostgresAdapter::class, CobrancaMapper::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:st_chk_adapter;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS CHECKOUT",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ]
)
class CobrancaRepositoryPostgresAdapterTest {

    @Autowired
    private lateinit var cobrancas: CobrancaRepositoryPostgresAdapter

    private fun nova(
        ordem: String = UUID.randomUUID().toString(),
        meio: MeioDePagamento = MeioDePagamento.PIX,
    ) = Cobranca.solicitar(ordem, meio, ValorMonetario(BigDecimal("267.70")))

    @Test
    fun `cobranca volta do banco igual ao que entrou`() {
        val cobranca = nova()

        cobrancas.salvar(cobranca)
        val lida = assertNotNull(cobrancas.porId(cobranca.id))

        assertEquals(cobranca.ordemServicoId, lida.ordemServicoId)
        assertEquals(MeioDePagamento.PIX, lida.meio)
        assertEquals(SituacaoDaCobranca.PENDENTE, lida.situacao)
        assertEquals(0, lida.valor.valor.compareTo(BigDecimal("267.70")))
        assertNull(lida.pagamentoExternoId)
    }

    @Test
    fun `cobranca inexistente volta nula`() {
        assertNull(cobrancas.porId(CobrancaId.gerar()))
    }

    @Test
    fun `regravar atualiza em vez de duplicar`() {
        val cobranca = nova()
        cobrancas.salvar(cobranca)

        val recarregada = assertNotNull(cobrancas.porId(cobranca.id))
        recarregada.registrarNoProvedor(1001, SituacaoDaCobranca.APROVADA, "accredited")
        cobrancas.salvar(recarregada)

        val lida = assertNotNull(cobrancas.porId(cobranca.id))
        assertEquals(1001, lida.pagamentoExternoId)
        assertEquals(SituacaoDaCobranca.APROVADA, lida.situacao)
        assertEquals(1, cobrancas.porOrdem(cobranca.ordemServicoId).size)
    }

    @Test
    fun `busca pelo identificador do provedor, que e o que a notificacao traz`() {
        val cobranca = nova()
        cobranca.registrarNoProvedor(2002, SituacaoDaCobranca.PENDENTE, "pending")
        cobrancas.salvar(cobranca)

        assertEquals(cobranca.id.valor, assertNotNull(cobrancas.porPagamentoExterno(2002)).id.valor)
        assertNull(cobrancas.porPagamentoExterno(9999))
    }

    @Test
    fun `busca por ordem e meio distingue cobrancas da mesma ordem`() {
        val ordem = UUID.randomUUID().toString()
        cobrancas.salvar(nova(ordem, MeioDePagamento.PIX))
        cobrancas.salvar(nova(ordem, MeioDePagamento.BOLETO))

        assertEquals(MeioDePagamento.PIX, assertNotNull(cobrancas.porOrdemEMeio(ordem, MeioDePagamento.PIX)).meio)
        assertEquals(MeioDePagamento.BOLETO, assertNotNull(cobrancas.porOrdemEMeio(ordem, MeioDePagamento.BOLETO)).meio)
        assertNull(cobrancas.porOrdemEMeio(ordem, MeioDePagamento.CARTAO))
        assertEquals(2, cobrancas.porOrdem(ordem).size)
    }

    @Test
    fun `motivo longo do provedor e truncado em vez de estourar a coluna`() {
        val cobranca = nova()
        cobranca.registrarNoProvedor(3003, SituacaoDaCobranca.RECUSADA, "x".repeat(900))

        cobrancas.salvar(cobranca)

        assertEquals(500, assertNotNull(cobrancas.porId(cobranca.id)).motivo!!.length)
    }

    @Test
    fun `notificacao registrada nao e reprocessada`() {
        assertTrue(!cobrancas.jaProcessada("payment:payment.updated:1001"))

        cobrancas.registrar("payment:payment.updated:1001", "payment", "payment.updated", "1001")

        assertTrue(cobrancas.jaProcessada("payment:payment.updated:1001"))
    }
}

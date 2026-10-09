package com.clau.service_track.checkout.application.handler

import com.clau.service_track.checkout.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.checkout.application.port.out.CobrancaRepositoryPort
import com.clau.service_track.checkout.application.port.out.DadosDoCartao
import com.clau.service_track.checkout.application.port.out.DadosDoPagador
import com.clau.service_track.checkout.application.port.out.PagamentoGatewayPort
import com.clau.service_track.checkout.application.port.out.RegistroDeNotificacaoPort
import com.clau.service_track.checkout.application.port.out.RespostaDoProvedor
import com.clau.service_track.checkout.domain.DomainException
import com.clau.service_track.checkout.domain.cobranca.Cobranca
import com.clau.service_track.checkout.domain.cobranca.MeioDePagamento
import com.clau.service_track.checkout.domain.cobranca.SituacaoDaCobranca
import com.clau.service_track.checkout.domain.vo.CobrancaId
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CobrancaCommandHandlerTest {

    private val guardadas = ConcurrentHashMap<String, Cobranca>()
    private val notificadas = ConcurrentHashMap<String, String>()
    private val chamadasAoProvedor = AtomicInteger()
    private val chavesDeIdempotencia = mutableListOf<String>()

    private var respostaDaCriacao: RespostaDoProvedor = aprovado(1001)
    private var respostaDaConsulta: RespostaDoProvedor = aprovado(1001)

    private val ordem = UUID.randomUUID().toString()

    private val pagador = DadosDoPagador(
        email = "joana@exemplo.test",
        primeiroNome = "Joana",
        sobrenome = "Ferreira",
        tipoDeDocumento = "CPF",
        numeroDoDocumento = "52998224725",
    )

    private fun aprovado(id: Long) = RespostaDoProvedor(id, SituacaoDaCobranca.APROVADA, "accredited")

    private val repositorio = object : CobrancaRepositoryPort {
        override fun salvar(cobranca: Cobranca): Cobranca {
            guardadas[cobranca.id.valor] = cobranca
            return cobranca
        }

        override fun porId(id: CobrancaId): Cobranca? = guardadas[id.valor]

        override fun porPagamentoExterno(idExterno: Long): Cobranca? =
            guardadas.values.firstOrNull { it.pagamentoExternoId == idExterno }

        override fun porOrdemEMeio(ordemServicoId: String, meio: MeioDePagamento): Cobranca? =
            guardadas.values.firstOrNull { it.ordemServicoId == ordemServicoId && it.meio == meio }

        override fun porOrdem(ordemServicoId: String): List<Cobranca> =
            guardadas.values.filter { it.ordemServicoId == ordemServicoId }
    }

    private val gateway = object : PagamentoGatewayPort {
        override fun cobrar(cobranca: Cobranca, pagador: DadosDoPagador, cartao: DadosDoCartao?): RespostaDoProvedor {
            chamadasAoProvedor.incrementAndGet()
            chavesDeIdempotencia.add("${cobranca.id.valor}:${cobranca.meio.name}")
            if (cobranca.meio == MeioDePagamento.CARTAO && cartao == null) {
                throw DomainException("Pagamento com cartão exige token, bandeira e parcelas")
            }
            return respostaDaCriacao
        }

        override fun consultar(idExterno: Long): RespostaDoProvedor {
            chamadasAoProvedor.incrementAndGet()
            return respostaDaConsulta
        }
    }

    private val registro = object : RegistroDeNotificacaoPort {
        override fun jaProcessada(chave: String): Boolean = notificadas.containsKey(chave)
        override fun registrar(chave: String, tipo: String, acao: String?, recursoId: String) {
            notificadas[chave] = recursoId
        }
    }

    private val handler = CobrancaCommandHandler(repositorio, gateway, registro)

    @BeforeTest
    fun preparar() {
        guardadas.clear()
        notificadas.clear()
        chamadasAoProvedor.set(0)
        chavesDeIdempotencia.clear()
        respostaDaCriacao = aprovado(1001)
        respostaDaConsulta = aprovado(1001)
    }

    private fun solicitar(
        meio: MeioDePagamento = MeioDePagamento.PIX,
        valor: String = "267.70",
        cartao: DadosDoCartao? = null,
    ) = handler.solicitar(ordem, meio, BigDecimal(valor), pagador, cartao)

    @Test
    fun `pix pendente guarda o identificador do provedor e devolve o QR`() {
        respostaDaCriacao = RespostaDoProvedor(
            idExterno = 2002,
            situacao = SituacaoDaCobranca.PENDENTE,
            detalhe = "pending_waiting_transfer",
            qrCode = "0002010102...",
        )

        val resultado = solicitar()

        assertEquals(SituacaoDaCobranca.PENDENTE, resultado.cobranca.situacao)
        assertEquals(2002, resultado.cobranca.pagamentoExternoId)
        assertEquals("0002010102...", resultado.provedor?.qrCode)
    }

    @Test
    fun `cartao aprovado ja volta aprovado, porque e sincrono`() {
        val resultado = solicitar(
            meio = MeioDePagamento.CARTAO,
            cartao = DadosDoCartao(token = "tok", bandeira = "master", parcelas = 1),
        )

        assertEquals(SituacaoDaCobranca.APROVADA, resultado.cobranca.situacao)
        assertEquals("accredited", resultado.cobranca.motivo)
    }

    @Test
    fun `cartao sem token e recusado antes de chegar ao provedor`() {
        assertFailsWith<DomainException> { solicitar(meio = MeioDePagamento.CARTAO) }
    }

    @Test
    fun `chave de idempotencia enviada ao provedor amarra cobranca e meio`() {
        val resultado = solicitar()

        assertEquals("${resultado.cobranca.id.valor}:PIX", chavesDeIdempotencia.single())
    }

    @Test
    fun `pedir duas vezes a mesma cobranca nao abre outra nem chama o provedor de novo`() {
        respostaDaCriacao = RespostaDoProvedor(3003, SituacaoDaCobranca.PENDENTE, "pending")

        val primeira = solicitar()
        val segunda = solicitar()

        assertEquals(primeira.cobranca.id.valor, segunda.cobranca.id.valor)
        assertEquals(1, chamadasAoProvedor.get())
        assertNull(segunda.provedor, "a segunda chamada nao fala com o provedor e nao tem QR novo")
    }

    @Test
    fun `cobranca recusada permite tentar de novo`() {
        respostaDaCriacao = RespostaDoProvedor(4004, SituacaoDaCobranca.RECUSADA, "cc_rejected_bad_filled_security_code")
        val primeira = solicitar(meio = MeioDePagamento.CARTAO, cartao = DadosDoCartao("tok", "master", 1))

        respostaDaCriacao = RespostaDoProvedor(4005, SituacaoDaCobranca.APROVADA, "accredited")
        val segunda = solicitar(meio = MeioDePagamento.CARTAO, cartao = DadosDoCartao("tok2", "master", 1))

        assertEquals(SituacaoDaCobranca.RECUSADA, primeira.cobranca.situacao)
        assertEquals(SituacaoDaCobranca.APROVADA, segunda.cobranca.situacao)
        assertEquals(2, chamadasAoProvedor.get())
    }

    @Test
    fun `valor com mais de duas casas e recusado`() {
        assertFailsWith<DomainException> { solicitar(valor = "10.999") }
    }

    @Test
    fun `notificacao consulta o provedor e aplica a situacao`() {
        respostaDaCriacao = RespostaDoProvedor(5005, SituacaoDaCobranca.PENDENTE, "pending")
        val criada = solicitar()

        respostaDaConsulta = RespostaDoProvedor(5005, SituacaoDaCobranca.APROVADA, "accredited")
        assertTrue(handler.aplicarNotificacao("payment", "payment.updated", "5005"))

        assertEquals(SituacaoDaCobranca.APROVADA, repositorio.porId(criada.cobranca.id)!!.situacao)
    }

    @Test
    fun `notificacao repetida nao reaplica nem consulta o provedor de novo`() {
        respostaDaCriacao = RespostaDoProvedor(6006, SituacaoDaCobranca.PENDENTE, "pending")
        solicitar()
        respostaDaConsulta = RespostaDoProvedor(6006, SituacaoDaCobranca.APROVADA, "accredited")

        handler.aplicarNotificacao("payment", "payment.updated", "6006")
        val chamadasDepois = chamadasAoProvedor.get()

        assertFalse(handler.aplicarNotificacao("payment", "payment.updated", "6006"))
        assertEquals(chamadasDepois, chamadasAoProvedor.get())
    }

    @Test
    fun `notificacao de pagamento desconhecido estoura recurso nao encontrado`() {
        assertFailsWith<RecursoNaoEncontradoException> {
            handler.aplicarNotificacao("payment", "payment.updated", "9999")
        }
    }

    @Test
    fun `notificacao com recurso que nao e numero estoura, em vez de buscar nada`() {
        assertFailsWith<RecursoNaoEncontradoException> {
            handler.aplicarNotificacao("payment", "payment.created", "nao-e-id")
        }
    }

    @Test
    fun `estorno aplica mesmo sobre cobranca ja aprovada`() {
        solicitar()
        respostaDaConsulta = RespostaDoProvedor(1001, SituacaoDaCobranca.ESTORNADA, "refunded")

        assertTrue(handler.aplicarNotificacao("payment", "payment.updated", "1001"))
    }

    @Test
    fun `recusa que chega depois da aprovacao e ignorada`() {
        solicitar()
        respostaDaConsulta = RespostaDoProvedor(1001, SituacaoDaCobranca.RECUSADA, "atrasada")

        assertFalse(
            handler.aplicarNotificacao("payment", "payment.updated", "1001"),
            "cobranca aprovada nao volta a recusada por notificacao fora de ordem",
        )
    }

    @Test
    fun `reconciliacao consulta o provedor e corrige o estado`() {
        respostaDaCriacao = RespostaDoProvedor(7007, SituacaoDaCobranca.PENDENTE, "pending")
        val criada = solicitar()

        respostaDaConsulta = RespostaDoProvedor(7007, SituacaoDaCobranca.APROVADA, "accredited")
        val reconciliada = handler.reconciliar(criada.cobranca.id)

        assertEquals(SituacaoDaCobranca.APROVADA, reconciliada.situacao)
    }

    @Test
    fun `reconciliacao de cobranca inexistente estoura`() {
        assertFailsWith<RecursoNaoEncontradoException> { handler.reconciliar(CobrancaId.gerar()) }
    }
}

package com.clau.service_track.checkout.infra.adapter.web

import com.clau.service_track.checkout.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.checkout.application.handler.CobrancaCommandHandler
import com.clau.service_track.checkout.application.handler.CobrancaQueryHandler
import com.clau.service_track.checkout.application.handler.ResultadoDaCobranca
import com.clau.service_track.checkout.application.port.out.CobrancaRepositoryPort
import com.clau.service_track.checkout.application.port.out.DadosDoCartao
import com.clau.service_track.checkout.application.port.out.DadosDoPagador
import com.clau.service_track.checkout.application.port.out.OrdensPort
import com.clau.service_track.checkout.application.port.out.PagamentoGatewayPort
import com.clau.service_track.checkout.application.port.out.RegistroDeNotificacaoPort
import com.clau.service_track.checkout.application.port.out.RespostaDoProvedor
import com.clau.service_track.checkout.domain.cobranca.Cobranca
import com.clau.service_track.checkout.domain.orcamento.OrcamentoAprovado
import com.clau.service_track.checkout.domain.vo.ValorMonetario
import com.clau.service_track.checkout.domain.cobranca.MeioDePagamento
import com.clau.service_track.checkout.domain.cobranca.SituacaoDaCobranca
import com.clau.service_track.checkout.domain.vo.CobrancaId
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.MercadoPagoUnavailableException
import com.clau.service_track.checkout.infra.adapter.web.error.GlobalExceptionHandler
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class CobrancaControllerTest {

    private val guardadas = ConcurrentHashMap<String, Cobranca>()
    private var resposta: RespostaDoProvedor = RespostaDoProvedor(1001, SituacaoDaCobranca.PENDENTE, "pending", qrCode = "000201")
    private var falharNoProvedor = false

    private val ordem = UUID.randomUUID().toString()

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
            if (falharNoProvedor) throw MercadoPagoUnavailableException(HttpStatus.BAD_GATEWAY, null)
            return resposta
        }
        override fun consultar(idExterno: Long): RespostaDoProvedor = resposta
    }

    private val registro = object : RegistroDeNotificacaoPort {
        override fun jaProcessada(chave: String) = false
        override fun registrar(chave: String, tipo: String, acao: String?, recursoId: String) = Unit
    }

    private val servicoDeOrdens = object : OrdensPort {
        override fun orcamentoAprovadoDe(ordemServicoId: String) = OrcamentoAprovado(
            ordemServicoId = ordemServicoId,
            orcamentoId = "orc-1",
            total = ValorMonetario(java.math.BigDecimal("267.70")),
        )
    }

    private lateinit var mockMvc: MockMvc

    @BeforeTest
    fun preparar() {
        guardadas.clear()
        falharNoProvedor = false
        resposta = RespostaDoProvedor(1001, SituacaoDaCobranca.PENDENTE, "pending", qrCode = "000201")

        val controller = CobrancaController(
            escrita = CobrancaCommandHandler(repositorio, gateway, registro, servicoDeOrdens),
            leitura = CobrancaQueryHandler(repositorio),
            mapper = CobrancaMapperWeb(),
        )

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    private fun corpo(meio: String = "PIX", extra: String = "") = """
        {
          "ordemServicoId": "$ordem",
          "meio": "$meio",
          "valor": 267.70,
          "pagador": {
            "email": "joana@exemplo.test",
            "primeiroNome": "Joana",
            "sobrenome": "Ferreira",
            "tipoDeDocumento": "CPF",
            "numeroDoDocumento": "52998224725"
          }$extra
        }
    """.trimIndent()

    @Test
    fun `pix responde 201 com o QR e em pendente`() {
        mockMvc.perform(post("/cobrancas").contentType(MediaType.APPLICATION_JSON).content(corpo()))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.situacao").value("PENDENTE"))
            .andExpect(jsonPath("$.qrCode").value("000201"))
            .andExpect(jsonPath("$.pagamentoExternoId").value(1001))
    }

    @Test
    fun `cartao sem bloco de cartao responde 422, nao 500`() {
        mockMvc.perform(
            post("/cobrancas").contentType(MediaType.APPLICATION_JSON).content(corpo(meio = "CARTAO"))
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.code").value("REGRA_DE_NEGOCIO"))
    }

    @Test
    fun `cartao com token responde 201`() {
        resposta = RespostaDoProvedor(2002, SituacaoDaCobranca.APROVADA, "accredited")
        val cartao = ""","cartao":{"token":"tok","bandeira":"master","parcelas":1}"""

        mockMvc.perform(
            post("/cobrancas").contentType(MediaType.APPLICATION_JSON).content(corpo("CARTAO", cartao))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.situacao").value("APROVADA"))
    }

    @Test
    fun `meio desconhecido responde 400 apontando o campo`() {
        mockMvc.perform(
            post("/cobrancas").contentType(MediaType.APPLICATION_JSON).content(corpo(meio = "CHEQUE"))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
    }

    @Test
    fun `documento com pontuacao responde 400`() {
        val comPontuacao = corpo().replace("\"52998224725\"", "\"529.982.247-25\"")

        mockMvc.perform(post("/cobrancas").contentType(MediaType.APPLICATION_JSON).content(comPontuacao))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `corpo que nao e json responde 400 com codigo proprio`() {
        mockMvc.perform(post("/cobrancas").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CORPO_ILEGIVEL"))
    }

    @Test
    fun `provedor fora do ar responde 503, e nao 500`() {
        falharNoProvedor = true

        mockMvc.perform(post("/cobrancas").contentType(MediaType.APPLICATION_JSON).content(corpo()))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.code").value("PROVEDOR_INDISPONIVEL"))
    }

    @Test
    fun `consulta por identificador e por ordem`() {
        mockMvc.perform(post("/cobrancas").contentType(MediaType.APPLICATION_JSON).content(corpo()))
            .andExpect(status().isCreated)

        val id = guardadas.values.single().id.valor

        mockMvc.perform(get("/cobrancas/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ordemServicoId").value(ordem))

        mockMvc.perform(get("/cobrancas").param("ordemServicoId", ordem))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `cobranca inexistente responde 404`() {
        mockMvc.perform(get("/cobrancas/${UUID.randomUUID()}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"))
    }

    @Test
    fun `reconciliacao aplica o que o provedor reporta`() {
        mockMvc.perform(post("/cobrancas").contentType(MediaType.APPLICATION_JSON).content(corpo()))
            .andExpect(status().isCreated)

        val id = guardadas.values.single().id.valor
        resposta = RespostaDoProvedor(1001, SituacaoDaCobranca.APROVADA, "accredited")

        mockMvc.perform(post("/cobrancas/$id/reconciliacao"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.situacao").value("APROVADA"))
    }
}

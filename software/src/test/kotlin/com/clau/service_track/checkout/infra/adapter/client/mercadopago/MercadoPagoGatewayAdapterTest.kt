package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.application.port.out.DadosDoCartao
import com.clau.service_track.checkout.application.port.out.DadosDoPagador
import com.clau.service_track.checkout.domain.DomainException
import com.clau.service_track.checkout.domain.cobranca.Cobranca
import com.clau.service_track.checkout.domain.cobranca.MeioDePagamento
import com.clau.service_track.checkout.domain.cobranca.SituacaoDaCobranca
import com.clau.service_track.checkout.domain.vo.ValorMonetario
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired

class MercadoPagoGatewayAdapterTest : MercadoPagoTestBase() {

    @Autowired
    private lateinit var gateway: MercadoPagoGatewayAdapter

    private val ordemServicoId = "0dec92ab-f782-4617-b1fe-c17aff05ff71"

    private val pagador = DadosDoPagador(
        email = "test@testuser.com",
        primeiroNome = "Joana",
        sobrenome = "Lima",
        tipoDeDocumento = "CPF",
        numeroDoDocumento = "52998224725",
    )

    private val pagadorComEndereco = pagador.copy(
        cep = "01310930",
        logradouro = "Av Paulista",
        numero = "1000",
        bairro = "Bela Vista",
        cidade = "Sao Paulo",
        uf = "SP",
    )

    private val cartao = DadosDoCartao(token = "token-do-cartao", bandeira = "master", parcelas = 3, emissor = "24")

    private fun cobranca(meio: MeioDePagamento, valor: String = "350.9") =
        Cobranca.solicitar(ordemServicoId, meio, ValorMonetario(BigDecimal(valor)))

    @Test
    fun `cobra pix como ordem online com um pagamento por transferencia`() {
        stub.enqueue(201, MercadoPagoPayloads.ORDEM_PIX_CRIADA)
        val cobranca = cobranca(MeioDePagamento.PIX)

        gateway.cobrar(cobranca, pagador, null)

        val corpo = corpoEnviado()
        assertEquals("POST", stub.lastRequest.method)
        assertEquals("/v1/orders", stub.lastRequest.path)
        assertEquals("online", corpo.path("type").asString())
        assertEquals("automatic", corpo.path("processing_mode").asString())
        assertEquals("350.90", corpo.path("total_amount").asString())
        assertEquals(cobranca.id.valor, corpo.path("external_reference").asString())
        assertEquals("Ordem de servico $ordemServicoId", corpo.path("description").asString())
        val pagamento = corpo.path("transactions").path("payments").path(0)
        assertEquals("350.90", pagamento.path("amount").asString())
        assertEquals("pix", pagamento.path("payment_method").path("id").asString())
        assertEquals("bank_transfer", pagamento.path("payment_method").path("type").asString())
        assertEquals("test@testuser.com", corpo.path("payer").path("email").asString())
        assertEquals("Joana", corpo.path("payer").path("first_name").asString())
        assertEquals("CPF", corpo.path("payer").path("identification").path("type").asString())
        assertEquals("52998224725", corpo.path("payer").path("identification").path("number").asString())
    }

    @Test
    fun `nao envia campos que a api de orders recusa`() {
        stub.enqueue(201, MercadoPagoPayloads.ORDEM_PIX_CRIADA)

        gateway.cobrar(cobranca(MeioDePagamento.PIX), pagador, null)

        val corpo = corpoEnviado()
        assertFalse(corpo.has("notification_url"))
        assertFalse(corpo.has("transaction_amount"))
        assertFalse(corpo.has("payment_method_id"))
        assertFalse(corpo.path("payer").has("address"))
        val meio = corpo.path("transactions").path("payments").path(0).path("payment_method")
        assertFalse(meio.has("token"))
        assertFalse(meio.has("installments"))
    }

    @Test
    fun `envia chave de idempotencia por cobranca e meio e o token de acesso`() {
        stub.enqueue(201, MercadoPagoPayloads.ORDEM_PIX_CRIADA)
        val cobranca = cobranca(MeioDePagamento.PIX)

        gateway.cobrar(cobranca, pagador, null)

        assertEquals("${cobranca.id.valor}:PIX", stub.lastRequest.headers.getFirst("X-Idempotency-Key"))
        assertEquals("Bearer access-token-de-teste", stub.lastRequest.headers.getFirst("Authorization"))
    }

    @Test
    fun `traduz a ordem pix criada em cobranca pendente com qr code`() {
        stub.enqueue(201, MercadoPagoPayloads.ORDEM_PIX_CRIADA)

        val resposta = gateway.cobrar(cobranca(MeioDePagamento.PIX), pagador, null)

        assertEquals("ORDTST01PIX0000000000000000001", resposta.idExterno)
        assertEquals(SituacaoDaCobranca.PENDENTE, resposta.situacao)
        assertEquals("waiting_transfer", resposta.detalhe)
        assertEquals("00020126580014br.gov.bcb.pix", resposta.qrCode)
        assertEquals("iVBORw0KGgoAAAANS", resposta.qrCodeBase64)
        assertNull(resposta.linkDoBoleto)
    }

    @Test
    fun `cobra boleto com endereco do pagador e devolve o link`() {
        stub.enqueue(201, MercadoPagoPayloads.ORDEM_BOLETO_CRIADA)

        val resposta = gateway.cobrar(cobranca(MeioDePagamento.BOLETO), pagadorComEndereco, null)

        val corpo = corpoEnviado()
        val meio = corpo.path("transactions").path("payments").path(0).path("payment_method")
        assertEquals("boleto", meio.path("id").asString())
        assertEquals("ticket", meio.path("type").asString())
        val endereco = corpo.path("payer").path("address")
        assertEquals("01310930", endereco.path("zip_code").asString())
        assertEquals("Av Paulista", endereco.path("street_name").asString())
        assertEquals("1000", endereco.path("street_number").asString())
        assertEquals("Bela Vista", endereco.path("neighborhood").asString())
        assertEquals("Sao Paulo", endereco.path("city").asString())
        assertEquals("SP", endereco.path("state").asString())
        assertFalse(endereco.has("federal_unit"))

        assertEquals(SituacaoDaCobranca.PENDENTE, resposta.situacao)
        assertEquals("https://www.mercadopago.com.br/payments/2/ticket", resposta.linkDoBoleto)
        assertNull(resposta.qrCode)
    }

    @Test
    fun `boleto sem endereco completo e recusado sem chamar o provedor`() {
        assertFailsWith<DomainException> {
            gateway.cobrar(cobranca(MeioDePagamento.BOLETO), pagador.copy(cep = "01310930"), null)
        }
        assertTrue(stub.requests.isEmpty())
    }

    @Test
    fun `cobra cartao com token, bandeira e parcelas e traduz ordem processada em aprovada`() {
        stub.enqueue(201, MercadoPagoPayloads.ORDEM_CARTAO_PROCESSADA)

        val resposta = gateway.cobrar(cobranca(MeioDePagamento.CARTAO), pagador, cartao)

        val meio = corpoEnviado().path("transactions").path("payments").path(0).path("payment_method")
        assertEquals("master", meio.path("id").asString())
        assertEquals("credit_card", meio.path("type").asString())
        assertEquals("token-do-cartao", meio.path("token").asString())
        assertEquals(3, meio.path("installments").asInt())
        assertFalse(meio.has("issuer_id"))

        assertEquals("ORDTST01CAR0000000000000000001", resposta.idExterno)
        assertEquals(SituacaoDaCobranca.APROVADA, resposta.situacao)
        assertEquals("accredited", resposta.detalhe)
    }

    @Test
    fun `cartao sem dados do cartao e recusado sem chamar o provedor`() {
        assertFailsWith<DomainException> {
            gateway.cobrar(cobranca(MeioDePagamento.CARTAO), pagador, null)
        }
        assertTrue(stub.requests.isEmpty())
    }

    @Test
    fun `formata o valor com duas casas e sem notacao cientifica`() {
        stub.enqueue(201, MercadoPagoPayloads.ORDEM_PIX_CRIADA)

        gateway.cobrar(cobranca(MeioDePagamento.PIX, "1E+3"), pagador, null)

        assertEquals("1000.00", corpoEnviado().path("total_amount").asString())
    }

    @Test
    fun `consulta a ordem pelo identificador e traduz o status`() {
        stub.enqueue(200, MercadoPagoPayloads.ORDEM_CANCELADA)

        val resposta = gateway.consultar("ORDTST01PIX0000000000000000001")

        assertEquals("GET", stub.lastRequest.method)
        assertEquals("/v1/orders/ORDTST01PIX0000000000000000001", stub.lastRequest.path)
        assertEquals(SituacaoDaCobranca.CANCELADA, resposta.situacao)
        assertEquals("canceled", resposta.detalhe)
    }

    @Test
    fun `sem detalhe na ordem usa o detalhe do pagamento`() {
        stub.enqueue(200, MercadoPagoPayloads.ORDEM_SEM_DETALHE)

        val resposta = gateway.consultar("ORDTST01PIX0000000000000000002")

        assertEquals(SituacaoDaCobranca.RECUSADA, resposta.situacao)
        assertEquals("rejected_by_issuer", resposta.detalhe)
    }

    @Test
    fun `recusa da api de orders vira excecao com a causa no formato novo`() {
        stub.enqueue(400, MercadoPagoPayloads.ERRO_REQUISICAO)

        val excecao = assertFailsWith<MercadoPagoRequestException> {
            gateway.cobrar(cobranca(MeioDePagamento.PIX), pagador, null)
        }

        assertEquals(400, excecao.statusCode.value())
        assertTrue(excecao.message!!.contains("unsupported_properties:Properties not supported"))
        assertTrue(excecao.message!!.contains("notification_url"))
    }

    @Test
    fun `recusa no formato antigo continua legivel`() {
        stub.enqueue(401, MercadoPagoPayloads.ERRO_CREDENCIAL)

        val excecao = assertFailsWith<MercadoPagoRequestException> {
            gateway.consultar("ORDTST01PIX0000000000000000001")
        }

        assertTrue(excecao.message!!.contains("7:Unauthorized use of live credentials"))
    }

    @Test
    fun `indisponibilidade vira excecao retentavel`() {
        stub.enqueue(500, MercadoPagoPayloads.ERRO_INDISPONIVEL)

        val excecao = assertFailsWith<MercadoPagoUnavailableException> {
            gateway.consultar("ORDTST01PIX0000000000000000001")
        }

        assertEquals("internal_error", excecao.error?.error)
    }

    @Test
    fun `aceita resposta de erro sem corpo json`() {
        stub.enqueue(503, "<html>indisponivel</html>")

        val excecao = assertFailsWith<MercadoPagoUnavailableException> {
            gateway.consultar("ORDTST01PIX0000000000000000001")
        }

        assertNull(excecao.error)
    }
}

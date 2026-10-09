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
import com.clau.service_track.checkout.domain.vo.ValorMonetario
import java.math.BigDecimal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CobrancaCommandHandler(
    private val cobrancas: CobrancaRepositoryPort,
    private val gateway: PagamentoGatewayPort,
    private val notificacoes: RegistroDeNotificacaoPort,
) {

    private val log = LoggerFactory.getLogger(CobrancaCommandHandler::class.java)

    @Transactional
    fun solicitar(
        ordemServicoId: String,
        meio: MeioDePagamento,
        valor: BigDecimal,
        pagador: DadosDoPagador,
        cartao: DadosDoCartao?,
    ): ResultadoDaCobranca {
        if (meio == MeioDePagamento.CARTAO && cartao == null) {
            throw DomainException("Pagamento com cartão exige token, bandeira e parcelas")
        }

        cobrancas.porOrdemEMeio(ordemServicoId, meio)?.let { existente ->
            if (!existente.situacao.encerrada || existente.situacao == SituacaoDaCobranca.APROVADA) {
                log.info(
                    "cobranca reaproveitada ordemServicoId={} meio={} situacao={}",
                    ordemServicoId, meio, existente.situacao,
                )
                return ResultadoDaCobranca(existente, null)
            }
        }

        val cobranca = cobrancas.salvar(
            Cobranca.solicitar(ordemServicoId, meio, ValorMonetario(valor))
        )

        val resposta = gateway.cobrar(cobranca, pagador, cartao)
        cobranca.registrarNoProvedor(resposta.idExterno, resposta.situacao, resposta.detalhe)

        val persistida = cobrancas.salvar(cobranca)
        log.info(
            "cobranca criada no provedor cobrancaId={} ordemServicoId={} meio={} pagamentoExternoId={} situacao={}",
            persistida.id.valor, ordemServicoId, meio, resposta.idExterno, resposta.situacao,
        )

        return ResultadoDaCobranca(persistida, resposta)
    }

    @Transactional
    fun aplicarNotificacao(tipo: String, acao: String?, recursoId: String): Boolean {
        val chave = "$tipo:${acao ?: "-"}:$recursoId"
        if (notificacoes.jaProcessada(chave)) {
            log.debug("notificacao ja processada chave={}", chave)
            return false
        }

        val idExterno = recursoId.toLongOrNull()
            ?: throw RecursoNaoEncontradoException("Notificacao com recurso '$recursoId' que nao e um pagamento")

        val cobranca = cobrancas.porPagamentoExterno(idExterno)
            ?: throw RecursoNaoEncontradoException("Nenhuma cobranca aponta para o pagamento $idExterno")

        val resposta = gateway.consultar(idExterno)
        val mudou = cobranca.atualizarPeloProvedor(resposta.situacao, resposta.detalhe)

        if (mudou) {
            cobrancas.salvar(cobranca)
            log.info(
                "cobranca atualizada por notificacao cobrancaId={} situacao={} motivo={}",
                cobranca.id.valor, resposta.situacao, resposta.detalhe,
            )
        }

        notificacoes.registrar(chave, tipo, acao, recursoId)
        return mudou
    }

    @Transactional
    fun reconciliar(id: CobrancaId): Cobranca {
        val cobranca = cobrancas.porId(id)
            ?: throw RecursoNaoEncontradoException("Cobranca ${id.valor} nao encontrada")

        val idExterno = cobranca.pagamentoExternoId
            ?: throw RecursoNaoEncontradoException(
                "Cobranca ${id.valor} nunca chegou ao provedor e nao tem o que reconciliar"
            )

        val resposta = gateway.consultar(idExterno)
        if (cobranca.atualizarPeloProvedor(resposta.situacao, resposta.detalhe)) {
            return cobrancas.salvar(cobranca)
        }
        return cobranca
    }
}

data class ResultadoDaCobranca(
    val cobranca: Cobranca,
    val provedor: RespostaDoProvedor?,
)

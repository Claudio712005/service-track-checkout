package com.clau.service_track.checkout.domain.cobranca

import com.clau.service_track.checkout.domain.DomainException
import com.clau.service_track.checkout.domain.vo.CobrancaId
import com.clau.service_track.checkout.domain.vo.ValorMonetario
import java.time.OffsetDateTime

class Cobranca private constructor(
    val id: CobrancaId,
    val ordemServicoId: String,
    val meio: MeioDePagamento,
    val valor: ValorMonetario,
    situacao: SituacaoDaCobranca,
    pagamentoExternoId: Long?,
    motivo: String?,
    val dataCriacao: OffsetDateTime,
    dataAtualizacao: OffsetDateTime,
) {

    var situacao: SituacaoDaCobranca = situacao
        private set

    var pagamentoExternoId: Long? = pagamentoExternoId
        private set

    var motivo: String? = motivo
        private set

    var dataAtualizacao: OffsetDateTime = dataAtualizacao
        private set

    companion object {

        fun solicitar(ordemServicoId: String, meio: MeioDePagamento, valor: ValorMonetario): Cobranca {
            if (ordemServicoId.isBlank()) {
                throw DomainException("Cobrança precisa da ordem de serviço que a originou")
            }

            val agora = OffsetDateTime.now()
            return Cobranca(
                id = CobrancaId.gerar(),
                ordemServicoId = ordemServicoId,
                meio = meio,
                valor = valor,
                situacao = SituacaoDaCobranca.PENDENTE,
                pagamentoExternoId = null,
                motivo = null,
                dataCriacao = agora,
                dataAtualizacao = agora,
            )
        }

        fun reconstituir(
            id: CobrancaId,
            ordemServicoId: String,
            meio: MeioDePagamento,
            valor: ValorMonetario,
            situacao: SituacaoDaCobranca,
            pagamentoExternoId: Long?,
            motivo: String?,
            dataCriacao: OffsetDateTime,
            dataAtualizacao: OffsetDateTime,
        ) = Cobranca(
            id, ordemServicoId, meio, valor, situacao, pagamentoExternoId, motivo, dataCriacao, dataAtualizacao,
        )
    }

    fun registrarNoProvedor(idExterno: Long, novaSituacao: SituacaoDaCobranca, detalhe: String?) {
        if (pagamentoExternoId != null && pagamentoExternoId != idExterno) {
            throw DomainException(
                "Cobrança ${id.valor} já aponta para o pagamento $pagamentoExternoId no provedor"
            )
        }
        pagamentoExternoId = idExterno
        aplicar(novaSituacao, detalhe)
    }

    fun atualizarPeloProvedor(novaSituacao: SituacaoDaCobranca, detalhe: String?): Boolean {
        if (situacao == novaSituacao) return false
        if (situacao.encerrada && novaSituacao != SituacaoDaCobranca.ESTORNADA) return false

        aplicar(novaSituacao, detalhe)
        return true
    }

    private fun aplicar(novaSituacao: SituacaoDaCobranca, detalhe: String?) {
        situacao = novaSituacao
        motivo = detalhe
        dataAtualizacao = OffsetDateTime.now()
    }
}

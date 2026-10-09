package com.clau.service_track.checkout.infra.adapter.repository

import com.clau.service_track.checkout.domain.cobranca.Cobranca
import com.clau.service_track.checkout.domain.cobranca.MeioDePagamento
import com.clau.service_track.checkout.domain.cobranca.SituacaoDaCobranca
import com.clau.service_track.checkout.domain.vo.CobrancaId
import com.clau.service_track.checkout.domain.vo.ValorMonetario
import com.clau.service_track.checkout.infra.entity.CobrancaEntity
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class CobrancaMapper {

    fun paraDominio(entidade: CobrancaEntity): Cobranca = Cobranca.reconstituir(
        id = CobrancaId.de(entidade.id.toString()),
        ordemServicoId = entidade.ordemServicoId.toString(),
        meio = MeioDePagamento.de(entidade.meio),
        valor = ValorMonetario(entidade.valor),
        situacao = SituacaoDaCobranca.de(entidade.situacao),
        pagamentoExternoId = entidade.pagamentoExternoId,
        motivo = entidade.motivo,
        dataCriacao = entidade.dataCriacao,
        dataAtualizacao = entidade.dataAtualizacao,
    )

    fun paraEntidade(cobranca: Cobranca, existente: CobrancaEntity?): CobrancaEntity {
        val entidade = existente ?: CobrancaEntity(
            id = UUID.fromString(cobranca.id.valor),
            ordemServicoId = UUID.fromString(cobranca.ordemServicoId),
            meio = cobranca.meio.name,
            valor = cobranca.valor.valor,
            situacao = cobranca.situacao.name,
            dataCriacao = cobranca.dataCriacao,
            dataAtualizacao = cobranca.dataAtualizacao,
        )

        entidade.situacao = cobranca.situacao.name
        entidade.pagamentoExternoId = cobranca.pagamentoExternoId
        entidade.motivo = cobranca.motivo?.take(500)
        entidade.dataAtualizacao = cobranca.dataAtualizacao
        return entidade
    }
}

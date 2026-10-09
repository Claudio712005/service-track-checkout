package com.clau.service_track.checkout.application.port.out

import com.clau.service_track.checkout.domain.cobranca.Cobranca
import com.clau.service_track.checkout.domain.cobranca.MeioDePagamento
import com.clau.service_track.checkout.domain.vo.CobrancaId

interface CobrancaRepositoryPort {

    fun salvar(cobranca: Cobranca): Cobranca

    fun porId(id: CobrancaId): Cobranca?

    fun porPagamentoExterno(idExterno: Long): Cobranca?

    fun porOrdemEMeio(ordemServicoId: String, meio: MeioDePagamento): Cobranca?

    fun porOrdem(ordemServicoId: String): List<Cobranca>
}

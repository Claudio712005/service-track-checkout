package com.clau.service_track.checkout.application.handler

import com.clau.service_track.checkout.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.checkout.application.port.out.CobrancaRepositoryPort
import com.clau.service_track.checkout.domain.cobranca.Cobranca
import com.clau.service_track.checkout.domain.vo.CobrancaId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CobrancaQueryHandler(
    private val cobrancas: CobrancaRepositoryPort,
) {

    @Transactional(readOnly = true)
    fun porId(id: CobrancaId): Cobranca = cobrancas.porId(id)
        ?: throw RecursoNaoEncontradoException("Cobranca ${id.valor} nao encontrada")

    @Transactional(readOnly = true)
    fun porOrdem(ordemServicoId: String): List<Cobranca> = cobrancas.porOrdem(ordemServicoId)
}

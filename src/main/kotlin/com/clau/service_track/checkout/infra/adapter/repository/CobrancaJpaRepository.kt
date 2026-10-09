package com.clau.service_track.checkout.infra.adapter.repository

import com.clau.service_track.checkout.infra.entity.CobrancaEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface CobrancaJpaRepository : JpaRepository<CobrancaEntity, UUID> {

    fun findByPagamentoExternoId(pagamentoExternoId: Long): CobrancaEntity?

    fun findByOrdemServicoIdAndMeio(ordemServicoId: UUID, meio: String): CobrancaEntity?

    fun findByOrdemServicoIdOrderByDataCriacaoDesc(ordemServicoId: UUID): List<CobrancaEntity>
}

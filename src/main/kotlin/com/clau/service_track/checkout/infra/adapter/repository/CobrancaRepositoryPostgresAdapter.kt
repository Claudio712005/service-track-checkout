package com.clau.service_track.checkout.infra.adapter.repository

import com.clau.service_track.checkout.application.port.out.CobrancaRepositoryPort
import com.clau.service_track.checkout.application.port.out.RegistroDeNotificacaoPort
import com.clau.service_track.checkout.domain.cobranca.Cobranca
import com.clau.service_track.checkout.domain.cobranca.MeioDePagamento
import com.clau.service_track.checkout.domain.vo.CobrancaId
import com.clau.service_track.checkout.infra.entity.NotificacaoEntity
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class CobrancaRepositoryPostgresAdapter(
    private val cobrancas: CobrancaJpaRepository,
    private val notificacoes: NotificacaoJpaRepository,
    private val mapper: CobrancaMapper,
) : CobrancaRepositoryPort, RegistroDeNotificacaoPort {

    @Transactional
    override fun salvar(cobranca: Cobranca): Cobranca {
        val existente = cobrancas.findById(UUID.fromString(cobranca.id.valor)).orElse(null)
        return mapper.paraDominio(cobrancas.save(mapper.paraEntidade(cobranca, existente)))
    }

    @Transactional(readOnly = true)
    override fun porId(id: CobrancaId): Cobranca? = cobrancas
        .findById(UUID.fromString(id.valor))
        .map(mapper::paraDominio)
        .orElse(null)

    @Transactional(readOnly = true)
    override fun porPagamentoExterno(idExterno: Long): Cobranca? = cobrancas
        .findByPagamentoExternoId(idExterno)
        ?.let(mapper::paraDominio)

    @Transactional(readOnly = true)
    override fun porOrdemEMeio(ordemServicoId: String, meio: MeioDePagamento): Cobranca? = cobrancas
        .findByOrdemServicoIdAndMeio(UUID.fromString(ordemServicoId), meio.name)
        ?.let(mapper::paraDominio)

    @Transactional(readOnly = true)
    override fun porOrdem(ordemServicoId: String): List<Cobranca> = cobrancas
        .findByOrdemServicoIdOrderByDataCriacaoDesc(UUID.fromString(ordemServicoId))
        .map(mapper::paraDominio)

    @Transactional(readOnly = true)
    override fun jaProcessada(chave: String): Boolean = notificacoes.existsById(chave)

    @Transactional
    override fun registrar(chave: String, tipo: String, acao: String?, recursoId: String) {
        notificacoes.save(
            NotificacaoEntity(
                id = chave,
                tipo = tipo,
                acao = acao,
                recursoId = recursoId,
                dataProcessamento = OffsetDateTime.now(ZoneOffset.UTC),
            )
        )
    }
}

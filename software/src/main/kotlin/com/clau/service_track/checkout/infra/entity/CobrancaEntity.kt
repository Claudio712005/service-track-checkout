package com.clau.service_track.checkout.infra.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "COBRANCAS", schema = "CHECKOUT")
class CobrancaEntity(

    @Id
    @Column(name = "ID", nullable = false)
    var id: UUID,

    @Column(name = "ORDEM_SERVICO_ID", nullable = false)
    var ordemServicoId: UUID,

    @Column(name = "MEIO", nullable = false, length = 20)
    var meio: String,

    @Column(name = "VALOR", nullable = false, precision = 12, scale = 2)
    var valor: BigDecimal,

    @Column(name = "SITUACAO", nullable = false, length = 20)
    var situacao: String,

    @Column(name = "PAGAMENTO_EXTERNO_ID")
    var pagamentoExternoId: Long? = null,

    @Column(name = "MOTIVO", length = 500)
    var motivo: String? = null,

    @Column(name = "DATA_CRIACAO", nullable = false)
    var dataCriacao: OffsetDateTime,

    @Column(name = "DATA_ATUALIZACAO", nullable = false)
    var dataAtualizacao: OffsetDateTime,

    @Version
    @Column(name = "VERSAO", nullable = false)
    var versao: Int = 0,
)

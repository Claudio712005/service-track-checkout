package com.clau.service_track.checkout.infra.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "NOTIFICACOES", schema = "CHECKOUT")
class NotificacaoEntity(

    @Id
    @Column(name = "ID", nullable = false, length = 120)
    var id: String,

    @Column(name = "TIPO", nullable = false, length = 40)
    var tipo: String,

    @Column(name = "ACAO", length = 60)
    var acao: String? = null,

    @Column(name = "RECURSO_ID", nullable = false, length = 60)
    var recursoId: String,

    @Column(name = "DATA_PROCESSAMENTO", nullable = false)
    var dataProcessamento: OffsetDateTime,
)

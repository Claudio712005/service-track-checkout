package com.clau.service_track.checkout.infra.adapter.web.webhook

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotificacaoDoProvedor(
    val type: String? = null,
    val action: String? = null,
    val data: DadosDaNotificacao? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DadosDaNotificacao(
    val id: String? = null,
)

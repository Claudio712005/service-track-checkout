package com.clau.service_track.checkout.infra.adapter.client.ordens

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrdemServicoResponse(
    val id: String,
    val status: String,
    val orcamento: OrcamentoResponse? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrcamentoResponse(
    val id: String,
    val valorTotal: BigDecimal,
    val aprovado: Boolean = false,
)

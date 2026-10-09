package com.clau.service_track.checkout.infra.adapter.web.error

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(name = "ErroResponse", description = "Corpo de toda resposta de erro deste servico.")
data class ErroResponse(
    val timestamp: OffsetDateTime,
    val status: Int,

    @get:Schema(description = "Classificacao estavel do erro, para o cliente decidir fluxo.")
    val code: String,
    val message: String,
    val path: String,
)

package com.clau.service_track.checkout.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.OffsetDateTime

@Schema(name = "CobrancaResponse", description = "Estado da cobrança neste serviço.")
data class CobrancaResponse(
    val id: String,
    val ordemServicoId: String,
    val meio: String,

    @get:Schema(description = "PENDENTE, EM_ANALISE, APROVADA, RECUSADA, CANCELADA ou ESTORNADA.")
    val situacao: String,
    val valor: BigDecimal,

    @get:Schema(description = "Identificador do pagamento no provedor.", nullable = true)
    val pagamentoExternoId: Long?,

    @get:Schema(description = "Detalhe que o provedor devolveu, sobretudo na recusa.", nullable = true)
    val motivo: String?,

    @get:Schema(description = "Copia e cola do Pix. Presente só em cobrança Pix recém-criada.", nullable = true)
    val qrCode: String? = null,

    @get:Schema(description = "QR em base64, para desenhar na tela.", nullable = true)
    val qrCodeBase64: String? = null,

    @get:Schema(description = "Link do boleto para impressão.", nullable = true)
    val linkDoBoleto: String? = null,

    val dataCriacao: OffsetDateTime,
    val dataAtualizacao: OffsetDateTime,
)

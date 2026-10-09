package com.clau.service_track.checkout.application.port.`in`.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

@Schema(name = "SolicitarCobrancaRequest", description = "Pedido de cobrança de uma ordem de serviço.")
data class SolicitarCobrancaRequest(

    @get:Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "deve ser um UUID")
    val ordemServicoId: String,

    @get:Schema(description = "PIX, BOLETO ou CARTAO.", example = "PIX")
    @get:Pattern(regexp = "^(PIX|BOLETO|CARTAO)$", message = "deve ser PIX, BOLETO ou CARTAO")
    val meio: String,

    @get:Schema(description = "Valor em reais, com duas casas decimais.", example = "267.70")
    @get:DecimalMin(value = "0.01", message = "deve ser maior que zero")
    @get:Digits(integer = 10, fraction = 2)
    val valor: BigDecimal,

    @get:Valid
    val pagador: PagadorRequest,

    @get:Schema(
        description = "Obrigatório apenas para CARTAO. O token vem do SDK no navegador: " +
            "**o número do cartão nunca passa por este serviço**.",
        nullable = true
    )
    @get:Valid
    val cartao: CartaoRequest? = null,
)

@Schema(name = "PagadorRequest", description = "Quem paga. Endereço é obrigatório só para boleto.")
data class PagadorRequest(

    @get:Email
    val email: String,

    @get:NotBlank
    @get:Size(max = 100)
    val primeiroNome: String,

    @get:NotBlank
    @get:Size(max = 100)
    val sobrenome: String,

    @get:Schema(description = "CPF ou CNPJ.", example = "CPF")
    @get:Pattern(regexp = "^(CPF|CNPJ)$", message = "deve ser CPF ou CNPJ")
    val tipoDeDocumento: String,

    @get:Pattern(regexp = "^[0-9]{11,14}$", message = "deve ter de 11 a 14 dígitos, sem pontuação")
    val numeroDoDocumento: String,

    @get:Pattern(regexp = "^[0-9]{8}$", message = "deve ter 8 dígitos, sem hífen")
    val cep: String? = null,
    @get:Size(max = 200) val logradouro: String? = null,
    @get:Size(max = 20) val numero: String? = null,
    @get:Size(max = 100) val bairro: String? = null,
    @get:Size(max = 100) val cidade: String? = null,

    @get:Pattern(regexp = "^[A-Z]{2}$", message = "deve ser a sigla da unidade federativa")
    val uf: String? = null,
)

@Schema(name = "CartaoRequest", description = "Dados tokenizados do cartão.")
data class CartaoRequest(

    @get:Schema(description = "Token gerado pelo SDK do provedor no cliente.")
    @get:NotBlank
    val token: String,

    @get:Schema(description = "Bandeira, no vocabulário do provedor.", example = "master")
    @get:NotBlank
    val bandeira: String,

    @get:Min(1)
    val parcelas: Int,

    val emissor: String? = null,
)

package com.clau.service_track.checkout.application.port.out

import com.clau.service_track.checkout.domain.cobranca.Cobranca
import com.clau.service_track.checkout.domain.cobranca.SituacaoDaCobranca

interface PagamentoGatewayPort {

    fun cobrar(cobranca: Cobranca, pagador: DadosDoPagador, cartao: DadosDoCartao?): RespostaDoProvedor

    fun consultar(idExterno: Long): RespostaDoProvedor
}

data class DadosDoPagador(
    val email: String,
    val primeiroNome: String,
    val sobrenome: String,
    val tipoDeDocumento: String,
    val numeroDoDocumento: String,
    val cep: String? = null,
    val logradouro: String? = null,
    val numero: String? = null,
    val bairro: String? = null,
    val cidade: String? = null,
    val uf: String? = null,
)

data class DadosDoCartao(
    val token: String,
    val bandeira: String,
    val parcelas: Int,
    val emissor: String? = null,
)

data class RespostaDoProvedor(
    val idExterno: Long,
    val situacao: SituacaoDaCobranca,
    val detalhe: String?,
    val qrCode: String? = null,
    val qrCodeBase64: String? = null,
    val linkDoBoleto: String? = null,
)

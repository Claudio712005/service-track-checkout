package com.clau.service_track.checkout.domain.cobranca

enum class MeioDePagamento(val sincrono: Boolean) {
    PIX(false),
    BOLETO(false),
    CARTAO(true);

    companion object {
        fun de(nome: String): MeioDePagamento = entries.find { it.name == nome.uppercase() }
            ?: throw IllegalArgumentException("Meio de pagamento desconhecido: $nome")
    }
}

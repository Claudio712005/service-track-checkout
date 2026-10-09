package com.clau.service_track.checkout.domain.cobranca

enum class SituacaoDaCobranca(val encerrada: Boolean) {
    PENDENTE(false),
    EM_ANALISE(false),
    APROVADA(true),
    RECUSADA(true),
    CANCELADA(true),
    ESTORNADA(true);

    companion object {

        fun de(nome: String): SituacaoDaCobranca = entries.find { it.name == nome }
            ?: throw IllegalArgumentException("Situacao de cobranca desconhecida: $nome")

        fun doMercadoPago(status: String?): SituacaoDaCobranca = when (status?.lowercase()) {
            "approved", "authorized" -> APROVADA
            "pending" -> PENDENTE
            "in_process", "in_mediation" -> EM_ANALISE
            "rejected" -> RECUSADA
            "cancelled" -> CANCELADA
            "refunded", "charged_back" -> ESTORNADA
            else -> throw IllegalArgumentException("Status '$status' nao mapeado para situacao de cobranca")
        }
    }
}

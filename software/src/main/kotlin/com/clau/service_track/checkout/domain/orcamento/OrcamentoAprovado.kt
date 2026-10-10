package com.clau.service_track.checkout.domain.orcamento

import com.clau.service_track.checkout.domain.DomainException
import com.clau.service_track.checkout.domain.vo.ValorMonetario

class OrcamentoAprovado(
    val ordemServicoId: String,
    val orcamentoId: String,
    val total: ValorMonetario,
) {

    fun exigirValorCobravel(valor: ValorMonetario) {
        if (valor.valor.compareTo(total.valor) != 0) {
            throw DomainException(
                "Valor da cobrança (${valor.valor}) não corresponde ao orçamento aprovado (${total.valor}) " +
                    "da ordem de serviço $ordemServicoId"
            )
        }
    }
}

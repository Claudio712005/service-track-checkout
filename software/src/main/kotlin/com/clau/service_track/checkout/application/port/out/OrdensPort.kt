package com.clau.service_track.checkout.application.port.out

import com.clau.service_track.checkout.domain.orcamento.OrcamentoAprovado

interface OrdensPort {

    fun orcamentoAprovadoDe(ordemServicoId: String): OrcamentoAprovado?
}

package com.clau.service_track.checkout.infra.adapter.client.ordens

import com.clau.service_track.checkout.application.port.out.OrdensPort
import com.clau.service_track.checkout.domain.orcamento.OrcamentoAprovado
import com.clau.service_track.checkout.domain.vo.ValorMonetario
import com.clau.service_track.checkout.infra.config.client.ClientsIntegrations
import com.clau.service_track.checkout.infra.config.client.IntegrationClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException

@Component
class OrdensHttpAdapter(
    private val integrationClient: IntegrationClient,
) : OrdensPort {

    private val log = LoggerFactory.getLogger(OrdensHttpAdapter::class.java)

    override fun orcamentoAprovadoDe(ordemServicoId: String): OrcamentoAprovado? {
        val ordem = try {
            integrationClient.call(ClientsIntegrations.CONSULTAR_ORDEM_SERVICO) { restClient ->
                restClient.get()
                    .uri("/{id}", ordemServicoId)
                    .retrieve()
                    .body(OrdemServicoResponse::class.java)
            }
        } catch (e: HttpClientErrorException.NotFound) {
            log.warn("ordem de servico {} nao existe no servico de ordens", ordemServicoId)
            return null
        }

        val orcamento = ordem?.orcamento ?: return null
        if (!orcamento.aprovado) {
            log.warn("ordem de servico {} tem orcamento nao aprovado", ordemServicoId)
            return null
        }

        return OrcamentoAprovado(
            ordemServicoId = ordemServicoId,
            orcamentoId = orcamento.id,
            total = ValorMonetario(orcamento.valorTotal),
        )
    }
}

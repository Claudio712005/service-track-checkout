package com.clau.service_track.checkout.infra.config.client

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "clients")
class ClientProperties(
    val integrations: Map<ClientsIntegrations, ClientDto> = mapOf(),
) {

    fun getClient(client: ClientsIntegrations): ClientDto =
        integrations[client] ?: throw IllegalStateException("client sem configuracao: ${client.name}")
}

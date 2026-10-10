package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class PayerResponse(
    val id: String? = null,

    val email: String? = null,

    val type: String? = null,

    @JsonProperty("first_name")
    val firstName: String? = null,

    @JsonProperty("last_name")
    val lastName: String? = null,

    @JsonProperty("entity_type")
    val entityType: String? = null,

    val identification: IdentificationResponse? = null,
)

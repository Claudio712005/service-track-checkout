package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PayerRequest(
    val email: String,

    @JsonProperty("first_name")
    val firstName: String? = null,

    @JsonProperty("last_name")
    val lastName: String? = null,

    @JsonProperty("entity_type")
    val entityType: String? = null,

    val type: String? = null,

    val identification: IdentificationRequest? = null,

    val phone: PhoneRequest? = null,

    val address: AddressRequest? = null,
)

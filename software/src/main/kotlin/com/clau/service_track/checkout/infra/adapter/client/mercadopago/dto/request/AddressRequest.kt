package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AddressRequest(
    @JsonProperty("zip_code")
    val zipCode: String? = null,

    @JsonProperty("street_name")
    val streetName: String? = null,

    @JsonProperty("street_number")
    val streetNumber: String? = null,

    val neighborhood: String? = null,

    val city: String? = null,

    @JsonProperty("federal_unit")
    val federalUnit: String? = null,

    val complement: String? = null,

    val floor: String? = null,
)

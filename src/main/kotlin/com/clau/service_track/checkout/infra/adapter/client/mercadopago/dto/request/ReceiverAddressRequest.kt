package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReceiverAddressRequest(
    @JsonProperty("zip_code")
    val zipCode: String? = null,

    @JsonProperty("street_name")
    val streetName: String? = null,

    @JsonProperty("street_number")
    val streetNumber: String? = null,

    @JsonProperty("state_name")
    val stateName: String? = null,

    @JsonProperty("city_name")
    val cityName: String? = null,

    val floor: String? = null,

    val apartment: String? = null,
)

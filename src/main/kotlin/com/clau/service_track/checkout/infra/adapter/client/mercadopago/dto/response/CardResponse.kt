package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class CardResponse(
    val id: String? = null,

    @JsonProperty("first_six_digits")
    val firstSixDigits: String? = null,

    @JsonProperty("last_four_digits")
    val lastFourDigits: String? = null,

    @JsonProperty("expiration_month")
    val expirationMonth: Int? = null,

    @JsonProperty("expiration_year")
    val expirationYear: Int? = null,

    val cardholder: CardholderResponse? = null,
)

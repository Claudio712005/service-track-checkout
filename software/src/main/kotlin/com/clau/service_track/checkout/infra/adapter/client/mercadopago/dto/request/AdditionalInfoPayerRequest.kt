package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdditionalInfoPayerRequest(
    @JsonProperty("first_name")
    val firstName: String? = null,

    @JsonProperty("last_name")
    val lastName: String? = null,

    val identification: IdentificationRequest? = null,

    val phone: PhoneRequest? = null,

    val address: AddressRequest? = null,

    @JsonProperty("registration_date")
    val registrationDate: OffsetDateTime? = null,

    @JsonProperty("is_prime_user")
    val primeUser: Boolean? = null,

    @JsonProperty("is_first_purchase_online")
    val firstPurchaseOnline: Boolean? = null,

    @JsonProperty("last_purchase")
    val lastPurchase: OffsetDateTime? = null,

    @JsonProperty("authentication_type")
    val authenticationType: String? = null,
)

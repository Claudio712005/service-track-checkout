package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ItemRequest(
    val id: String? = null,

    val title: String? = null,

    val description: String? = null,

    val type: String? = null,

    @JsonProperty("picture_url")
    val pictureUrl: String? = null,

    @JsonProperty("category_id")
    val categoryId: String? = null,

    @JsonProperty("currency_id")
    val currencyId: String? = null,

    val quantity: Int? = null,

    @JsonProperty("unit_price")
    val unitPrice: BigDecimal? = null,

    val warranty: Boolean? = null,

    @JsonProperty("event_date")
    val eventDate: OffsetDateTime? = null,
)

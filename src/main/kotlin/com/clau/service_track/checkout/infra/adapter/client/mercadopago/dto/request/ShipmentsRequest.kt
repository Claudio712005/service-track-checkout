package com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ShipmentsRequest(
    @JsonProperty("receiver_address")
    val receiverAddress: ReceiverAddressRequest? = null,

    @JsonProperty("local_pickup")
    val localPickup: Boolean? = null,

    @JsonProperty("express_shipment")
    val expressShipment: Boolean? = null,
)

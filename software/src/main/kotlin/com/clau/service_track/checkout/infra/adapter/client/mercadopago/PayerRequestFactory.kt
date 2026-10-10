package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.PayerAddressInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.PayerInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request.AddressRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request.IdentificationRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request.PayerRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.request.PhoneRequest

internal object PayerRequestFactory {

    fun payerRequest(input: PayerInput, address: PayerAddressInput? = null): PayerRequest =
        PayerRequest(
            email = input.email,
            firstName = input.firstName,
            lastName = input.lastName,
            identification = IdentificationRequest(
                type = input.documentType,
                number = input.documentNumber,
            ),
            phone = phoneRequest(input),
            address = address?.let(::addressRequest),
        )

    private fun phoneRequest(input: PayerInput): PhoneRequest? =
        if (input.areaCode == null && input.phoneNumber == null) {
            null
        } else {
            PhoneRequest(areaCode = input.areaCode, number = input.phoneNumber)
        }

    private fun addressRequest(address: PayerAddressInput): AddressRequest =
        AddressRequest(
            zipCode = address.zipCode,
            streetName = address.streetName,
            streetNumber = address.streetNumber,
            neighborhood = address.neighborhood,
            city = address.city,
            federalUnit = address.federalUnit,
        )
}

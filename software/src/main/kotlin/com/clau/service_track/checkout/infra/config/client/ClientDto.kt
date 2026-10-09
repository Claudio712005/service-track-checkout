package com.clau.service_track.checkout.infra.config.client

data class ClientDto(
    val url: String,
    val timeout: Int,
    val connectTimeout: Int = 5000,
    val retry: Int = 0,
)

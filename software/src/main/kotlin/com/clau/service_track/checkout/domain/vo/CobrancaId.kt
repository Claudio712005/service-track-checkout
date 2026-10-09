package com.clau.service_track.checkout.domain.vo

import java.util.UUID

@JvmInline
value class CobrancaId private constructor(val valor: String) {
    companion object {
        fun gerar() = CobrancaId(UUID.randomUUID().toString())

        fun de(valor: String) = CobrancaId(valor)
    }
}

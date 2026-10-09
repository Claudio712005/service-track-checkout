package com.clau.service_track.checkout.domain.vo

import com.clau.service_track.checkout.domain.DomainException
import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class ValorMonetario(val valor: BigDecimal) {

    init {
        if (valor <= BigDecimal.ZERO) {
            throw DomainException("Valor da cobrança deve ser maior que zero")
        }
        if (valor.scale() > 2) {
            throw DomainException("Valor da cobrança não pode ter mais de duas casas decimais")
        }
    }

    companion object {
        fun de(bruto: String) = ValorMonetario(BigDecimal(bruto).setScale(2, RoundingMode.UNNECESSARY))
    }
}

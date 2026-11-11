package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.application.port.out.DadosDoCartao
import com.clau.service_track.checkout.application.port.out.DadosDoPagador
import com.clau.service_track.checkout.domain.DomainException
import com.clau.service_track.checkout.domain.cobranca.Cobranca
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.order.OrderAddressRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.order.OrderIdentificationRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.order.OrderPayerRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.order.OrderPaymentMethodRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.order.OrderPaymentRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.order.OrderRequest
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.order.OrderTransactionsRequest
import java.math.RoundingMode

internal object OrderRequestFactory {

    private const val PIX = "pix"
    private const val BANK_TRANSFER = "bank_transfer"
    private const val BOLETO = "boleto"
    private const val TICKET = "ticket"
    private const val CREDIT_CARD = "credit_card"

    fun pix(cobranca: Cobranca, pagador: DadosDoPagador): OrderRequest =
        order(cobranca, payer(pagador), OrderPaymentMethodRequest(id = PIX, type = BANK_TRANSFER))

    fun boleto(cobranca: Cobranca, pagador: DadosDoPagador): OrderRequest =
        order(
            cobranca,
            payer(pagador).copy(address = address(pagador)),
            OrderPaymentMethodRequest(id = BOLETO, type = TICKET),
        )

    fun cartao(cobranca: Cobranca, pagador: DadosDoPagador, cartao: DadosDoCartao): OrderRequest =
        order(
            cobranca,
            payer(pagador),
            OrderPaymentMethodRequest(
                id = cartao.bandeira,
                type = CREDIT_CARD,
                token = cartao.token,
                installments = cartao.parcelas,
            ),
        )

    private fun order(
        cobranca: Cobranca,
        payer: OrderPayerRequest,
        paymentMethod: OrderPaymentMethodRequest,
    ): OrderRequest {
        val amount = amountOf(cobranca)
        return OrderRequest(
            totalAmount = amount,
            externalReference = cobranca.id.valor,
            description = "Ordem de servico ${cobranca.ordemServicoId}",
            payer = payer,
            transactions = OrderTransactionsRequest(
                payments = listOf(OrderPaymentRequest(amount = amount, paymentMethod = paymentMethod)),
            ),
        )
    }

    private fun amountOf(cobranca: Cobranca): String =
        cobranca.valor.valor.setScale(2, RoundingMode.HALF_UP).toPlainString()

    private fun payer(pagador: DadosDoPagador) = OrderPayerRequest(
        email = pagador.email,
        firstName = pagador.primeiroNome,
        lastName = pagador.sobrenome,
        identification = OrderIdentificationRequest(
            type = pagador.tipoDeDocumento,
            number = pagador.numeroDoDocumento,
        ),
    )

    private fun address(pagador: DadosDoPagador): OrderAddressRequest {
        val cep = pagador.cep
        val logradouro = pagador.logradouro
        val numero = pagador.numero
        val bairro = pagador.bairro
        val cidade = pagador.cidade
        val uf = pagador.uf

        if (cep == null || logradouro == null || numero == null || bairro == null || cidade == null || uf == null) {
            throw DomainException(
                "Boleto exige endereço completo do pagador: CEP, logradouro, número, bairro, cidade e UF"
            )
        }

        return OrderAddressRequest(
            zipCode = cep,
            streetName = logradouro,
            streetNumber = numero,
            neighborhood = bairro,
            city = cidade,
            state = uf,
        )
    }
}

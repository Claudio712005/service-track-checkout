package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.application.port.out.DadosDoCartao
import com.clau.service_track.checkout.application.port.out.DadosDoPagador
import com.clau.service_track.checkout.application.port.out.PagamentoGatewayPort
import com.clau.service_track.checkout.application.port.out.RespostaDoProvedor
import com.clau.service_track.checkout.domain.DomainException
import com.clau.service_track.checkout.domain.cobranca.Cobranca
import com.clau.service_track.checkout.domain.cobranca.MeioDePagamento
import com.clau.service_track.checkout.domain.cobranca.SituacaoDaCobranca
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.order.OrderResponse
import org.springframework.stereotype.Component

@Component
class MercadoPagoGatewayAdapter(
    private val orders: MercadoPagoOrderClient,
) : PagamentoGatewayPort {

    override fun cobrar(
        cobranca: Cobranca,
        pagador: DadosDoPagador,
        cartaoDoPagador: DadosDoCartao?,
    ): RespostaDoProvedor {
        val pedido = when (cobranca.meio) {
            MeioDePagamento.PIX -> OrderRequestFactory.pix(cobranca, pagador)
            MeioDePagamento.BOLETO -> OrderRequestFactory.boleto(cobranca, pagador)
            MeioDePagamento.CARTAO -> {
                val dados = cartaoDoPagador
                    ?: throw DomainException("Pagamento com cartão exige token, bandeira e parcelas")
                OrderRequestFactory.cartao(cobranca, pagador, dados)
            }
        }

        return toResposta(orders.create(pedido, chaveDeIdempotencia(cobranca)))
    }

    override fun consultar(idExterno: String): RespostaDoProvedor = toResposta(orders.find(idExterno))

    private fun chaveDeIdempotencia(cobranca: Cobranca) = "${cobranca.id.valor}:${cobranca.meio.name}"

    private fun toResposta(ordem: OrderResponse): RespostaDoProvedor {
        val meio = ordem.payment?.paymentMethod
        return RespostaDoProvedor(
            idExterno = ordem.id,
            situacao = SituacaoDaCobranca.doMercadoPago(ordem.status),
            detalhe = ordem.statusDetail ?: ordem.payment?.statusDetail,
            qrCode = meio?.qrCode,
            qrCodeBase64 = meio?.qrCodeBase64,
            linkDoBoleto = meio?.ticketUrl.takeIf { meio?.type == TICKET },
        )
    }

    private companion object {
        const val TICKET = "ticket"
    }
}

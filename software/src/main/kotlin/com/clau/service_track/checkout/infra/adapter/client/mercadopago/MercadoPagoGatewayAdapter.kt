package com.clau.service_track.checkout.infra.adapter.client.mercadopago

import com.clau.service_track.checkout.application.port.out.DadosDoCartao
import com.clau.service_track.checkout.application.port.out.DadosDoPagador
import com.clau.service_track.checkout.application.port.out.PagamentoGatewayPort
import com.clau.service_track.checkout.application.port.out.RespostaDoProvedor
import com.clau.service_track.checkout.domain.DomainException
import com.clau.service_track.checkout.domain.cobranca.Cobranca
import com.clau.service_track.checkout.domain.cobranca.MeioDePagamento
import com.clau.service_track.checkout.domain.cobranca.SituacaoDaCobranca
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.BoletoCheckoutInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.CartaoCheckoutInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.PayerAddressInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.PayerInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.input.PixCheckoutInput
import com.clau.service_track.checkout.infra.adapter.client.mercadopago.dto.response.PaymentResponse
import org.springframework.stereotype.Component

@Component
class MercadoPagoGatewayAdapter(
    private val pix: PixCheckout,
    private val boleto: BoletoCheckout,
    private val cartao: CartaoCheckout,
    private val consulta: MercadoPagoPaymentClient,
) : PagamentoGatewayPort {

    override fun cobrar(
        cobranca: Cobranca,
        pagador: DadosDoPagador,
        cartaoDoPagador: DadosDoCartao?,
    ): RespostaDoProvedor {
        val descricao = "Ordem de servico ${cobranca.ordemServicoId}"
        val resposta = when (cobranca.meio) {
            MeioDePagamento.PIX -> pix.send(
                PixCheckoutInput(
                    transactionAmount = cobranca.valor.valor,
                    description = descricao,
                    externalReference = cobranca.id.valor,
                    payer = pagadorDe(pagador),
                ),
                chaveDeIdempotencia(cobranca),
            )

            MeioDePagamento.BOLETO -> boleto.send(
                BoletoCheckoutInput(
                    transactionAmount = cobranca.valor.valor,
                    description = descricao,
                    externalReference = cobranca.id.valor,
                    payer = pagadorDe(pagador),
                    address = enderecoDe(pagador),
                ),
                chaveDeIdempotencia(cobranca),
            )

            MeioDePagamento.CARTAO -> {
                val dados = cartaoDoPagador
                    ?: throw DomainException("Pagamento com cartão exige token, bandeira e parcelas")

                cartao.send(
                    CartaoCheckoutInput(
                        transactionAmount = cobranca.valor.valor,
                        description = descricao,
                        externalReference = cobranca.id.valor,
                        payer = pagadorDe(pagador),
                        token = dados.token,
                        paymentMethodId = dados.bandeira,
                        installments = dados.parcelas,
                        issuerId = dados.emissor,
                    ),
                    chaveDeIdempotencia(cobranca),
                )
            }
        }

        return paraResposta(resposta)
    }

    override fun consultar(idExterno: Long): RespostaDoProvedor = paraResposta(consulta.find(idExterno))

    private fun chaveDeIdempotencia(cobranca: Cobranca) = "${cobranca.id.valor}:${cobranca.meio.name}"

    private fun pagadorDe(pagador: DadosDoPagador) = PayerInput(
        email = pagador.email,
        firstName = pagador.primeiroNome,
        lastName = pagador.sobrenome,
        documentType = pagador.tipoDeDocumento,
        documentNumber = pagador.numeroDoDocumento,
    )

    private fun enderecoDe(pagador: DadosDoPagador): PayerAddressInput {
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

        return PayerAddressInput(
            zipCode = cep,
            streetName = logradouro,
            streetNumber = numero,
            neighborhood = bairro,
            city = cidade,
            federalUnit = uf,
        )
    }

    private fun paraResposta(resposta: PaymentResponse) = RespostaDoProvedor(
        idExterno = resposta.id,
        situacao = SituacaoDaCobranca.doMercadoPago(resposta.status),
        detalhe = resposta.statusDetail,
        qrCode = resposta.pointOfInteraction?.transactionData?.qrCode,
        qrCodeBase64 = resposta.pointOfInteraction?.transactionData?.qrCodeBase64,
        linkDoBoleto = resposta.transactionDetails?.externalResourceUrl,
    )
}

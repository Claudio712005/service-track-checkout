package com.clau.service_track.checkout.infra.adapter.web

import com.clau.service_track.checkout.application.handler.ResultadoDaCobranca
import com.clau.service_track.checkout.application.port.`in`.api.dto.CobrancaResponse
import com.clau.service_track.checkout.application.port.`in`.api.dto.PagadorRequest
import com.clau.service_track.checkout.application.port.`in`.api.dto.SolicitarCobrancaRequest
import com.clau.service_track.checkout.application.port.out.DadosDoCartao
import com.clau.service_track.checkout.application.port.out.DadosDoPagador
import com.clau.service_track.checkout.domain.cobranca.Cobranca
import org.springframework.stereotype.Component

@Component
class CobrancaMapperWeb {

    fun pagadorDe(requisicao: PagadorRequest) = DadosDoPagador(
        email = requisicao.email,
        primeiroNome = requisicao.primeiroNome,
        sobrenome = requisicao.sobrenome,
        tipoDeDocumento = requisicao.tipoDeDocumento,
        numeroDoDocumento = requisicao.numeroDoDocumento,
        cep = requisicao.cep,
        logradouro = requisicao.logradouro,
        numero = requisicao.numero,
        bairro = requisicao.bairro,
        cidade = requisicao.cidade,
        uf = requisicao.uf,
    )

    fun cartaoDe(requisicao: SolicitarCobrancaRequest): DadosDoCartao? = requisicao.cartao?.let {
        DadosDoCartao(
            token = it.token,
            bandeira = it.bandeira,
            parcelas = it.parcelas,
            emissor = it.emissor,
        )
    }

    fun paraResposta(resultado: ResultadoDaCobranca): CobrancaResponse = paraResposta(resultado.cobranca).copy(
        qrCode = resultado.provedor?.qrCode,
        qrCodeBase64 = resultado.provedor?.qrCodeBase64,
        linkDoBoleto = resultado.provedor?.linkDoBoleto,
    )

    fun paraResposta(cobranca: Cobranca) = CobrancaResponse(
        id = cobranca.id.valor,
        ordemServicoId = cobranca.ordemServicoId,
        meio = cobranca.meio.name,
        situacao = cobranca.situacao.name,
        valor = cobranca.valor.valor,
        pagamentoExternoId = cobranca.pagamentoExternoId,
        motivo = cobranca.motivo,
        dataCriacao = cobranca.dataCriacao,
        dataAtualizacao = cobranca.dataAtualizacao,
    )
}

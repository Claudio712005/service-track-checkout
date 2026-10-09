package com.clau.service_track.checkout.infra.adapter.web

import com.clau.service_track.checkout.application.handler.CobrancaCommandHandler
import com.clau.service_track.checkout.application.handler.CobrancaQueryHandler
import com.clau.service_track.checkout.application.port.`in`.api.CobrancaApiPort
import com.clau.service_track.checkout.application.port.`in`.api.dto.CobrancaResponse
import com.clau.service_track.checkout.application.port.`in`.api.dto.SolicitarCobrancaRequest
import com.clau.service_track.checkout.domain.cobranca.MeioDePagamento
import com.clau.service_track.checkout.domain.vo.CobrancaId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class CobrancaController(
    private val escrita: CobrancaCommandHandler,
    private val leitura: CobrancaQueryHandler,
    private val mapper: CobrancaMapperWeb,
) : CobrancaApiPort {

    override fun solicitar(requisicao: SolicitarCobrancaRequest): ResponseEntity<CobrancaResponse> {
        val resultado = escrita.solicitar(
            ordemServicoId = requisicao.ordemServicoId,
            meio = MeioDePagamento.de(requisicao.meio),
            valor = requisicao.valor,
            pagador = mapper.pagadorDe(requisicao.pagador),
            cartao = mapper.cartaoDe(requisicao),
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.paraResposta(resultado))
    }

    override fun porId(id: String): ResponseEntity<CobrancaResponse> =
        ResponseEntity.ok(mapper.paraResposta(leitura.porId(CobrancaId.de(id))))

    override fun porOrdem(ordemServicoId: String): ResponseEntity<List<CobrancaResponse>> =
        ResponseEntity.ok(leitura.porOrdem(ordemServicoId).map(mapper::paraResposta))

    override fun reconciliar(id: String): ResponseEntity<CobrancaResponse> =
        ResponseEntity.ok(mapper.paraResposta(escrita.reconciliar(CobrancaId.de(id))))
}

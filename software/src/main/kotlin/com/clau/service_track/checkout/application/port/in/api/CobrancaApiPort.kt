package com.clau.service_track.checkout.application.port.`in`.api

import com.clau.service_track.checkout.application.port.`in`.api.dto.CobrancaResponse
import com.clau.service_track.checkout.application.port.`in`.api.dto.SolicitarCobrancaRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(
    name = "Cobranças",
    description = "Cobrança de uma ordem de serviço por Pix, boleto ou cartão. O número do cartão " +
        "nunca passa por aqui: o cliente tokeniza no navegador e envia o token."
)
@RequestMapping("/cobrancas")
interface CobrancaApiPort {

    @Operation(
        summary = "Solicita uma cobrança",
        description = "**Pix e boleto são assíncronos**: a resposta sai em PENDENTE com o QR ou o link, e a " +
            "aprovação chega depois por notificação do provedor. **Cartão responde na própria chamada**, " +
            "aprovado ou recusado. Chamar duas vezes para a mesma ordem e o mesmo meio devolve a cobrança " +
            "existente em vez de abrir outra."
    )
    @ApiResponse(responseCode = "201", description = "Cobrança criada no provedor")
    @ApiResponse(responseCode = "400", description = "Corpo inválido", content = [])
    @ApiResponse(responseCode = "422", description = "Regra de negócio recusou a cobrança", content = [])
    @ApiResponse(responseCode = "503", description = "Provedor indisponível", content = [])
    @PostMapping
    fun solicitar(@Valid @RequestBody requisicao: SolicitarCobrancaRequest): ResponseEntity<CobrancaResponse>

    @Operation(summary = "Consulta uma cobrança")
    @ApiResponse(responseCode = "404", description = "Cobrança inexistente", content = [])
    @GetMapping("/{id}")
    fun porId(@PathVariable id: String): ResponseEntity<CobrancaResponse>

    @Operation(summary = "Lista as cobranças de uma ordem de serviço")
    @GetMapping
    fun porOrdem(@RequestParam ordemServicoId: String): ResponseEntity<List<CobrancaResponse>>

    @Operation(
        summary = "Reconcilia a cobrança com o provedor",
        description = "Consulta o provedor e aplica o estado que ele reporta. Existe porque notificação pode " +
            "não chegar — ambiente efêmero perde a URL registrada no provedor a cada recriação — e porque o " +
            "processo pode ter morrido entre criar a cobrança e receber a resposta."
    )
    @PostMapping("/{id}/reconciliacao")
    fun reconciliar(@PathVariable id: String): ResponseEntity<CobrancaResponse>
}

package com.clau.service_track.checkout.application.port.out

interface RegistroDeNotificacaoPort {

    fun jaProcessada(chave: String): Boolean

    fun registrar(chave: String, tipo: String, acao: String?, recursoId: String)
}

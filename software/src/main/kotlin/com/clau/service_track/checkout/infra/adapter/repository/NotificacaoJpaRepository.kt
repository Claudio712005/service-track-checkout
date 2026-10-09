package com.clau.service_track.checkout.infra.adapter.repository

import com.clau.service_track.checkout.infra.entity.NotificacaoEntity
import org.springframework.data.jpa.repository.JpaRepository

interface NotificacaoJpaRepository : JpaRepository<NotificacaoEntity, String>

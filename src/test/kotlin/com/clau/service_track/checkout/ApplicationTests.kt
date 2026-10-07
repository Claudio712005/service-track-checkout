package com.clau.service_track.checkout

import kotlin.test.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:st_chk;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.otlp.tracing.export.enabled=false",
        "management.otlp.metrics.export.enabled=false",
    ],
)
class ApplicationTests {

    @Test
    fun `o contexto sobe`() {
    }
}

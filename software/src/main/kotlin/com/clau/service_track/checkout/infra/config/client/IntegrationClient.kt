package com.clau.service_track.checkout.infra.config.client

import com.clau.service_track.checkout.infra.adapter.web.filter.CorrelationFilter
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.MDC
import org.springframework.core.retry.RetryException
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient

@Component
class IntegrationClient(
    private val clientProperties: ClientProperties,
    private val restClientBuilder: RestClient.Builder,
) {

    private val restClients = ConcurrentHashMap<ClientsIntegrations, RestClient>()
    private val retryTemplates = ConcurrentHashMap<ClientsIntegrations, RetryTemplate>()

    fun <T> call(integration: ClientsIntegrations, block: (RestClient) -> T): T {
        val settings = clientProperties.getClient(integration)
        val restClient = restClients.computeIfAbsent(integration) { restClient(settings) }

        if (settings.retry <= 0) {
            return block(restClient)
        }

        val retryTemplate = retryTemplates.computeIfAbsent(integration) { retryTemplate(settings) }

        return try {
            retryTemplate.execute { block(restClient) }
        } catch (exception: RetryException) {
            throw exception.cause ?: exception
        }
    }

    private fun restClient(settings: ClientDto): RestClient {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(settings.connectTimeout.toLong()))
            .build()

        val requestFactory = JdkClientHttpRequestFactory(httpClient)
        requestFactory.setReadTimeout(Duration.ofMillis(settings.timeout.toLong()))

        return restClientBuilder.clone()
            .baseUrl(settings.url)
            .requestFactory(requestFactory)
            .requestInterceptor { request, body, execution ->
                forwardCorrelation(request.headers)
                execution.execute(request, body)
            }
            .build()
    }

    private fun forwardCorrelation(headers: HttpHeaders) {
        MDC.get(CorrelationFilter.CORRELATION_FIELD)
            ?.takeIf { it.isNotBlank() }
            ?.let { headers.set(CorrelationFilter.CORRELATION_HEADER, it) }
        MDC.get(CorrelationFilter.REQUEST_FIELD)
            ?.takeIf { it.isNotBlank() }
            ?.let { headers.set(CorrelationFilter.REQUEST_HEADER, it) }
    }

    private fun retryTemplate(settings: ClientDto): RetryTemplate =
        RetryTemplate(
            RetryPolicy.builder()
                .maxRetries(settings.retry.toLong())
                .delay(INITIAL_DELAY)
                .multiplier(BACKOFF_MULTIPLIER)
                .maxDelay(MAX_DELAY)
                .predicate { it is ResourceAccessException || it is RetryableIntegrationFailure }
                .build(),
        )

    private companion object {
        val INITIAL_DELAY: Duration = Duration.ofMillis(300)
        val MAX_DELAY: Duration = Duration.ofSeconds(3)
        const val BACKOFF_MULTIPLIER = 2.0
    }
}

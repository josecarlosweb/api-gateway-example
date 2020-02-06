package com.ccarvalho.apigateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class BooleanAnswerFilter: AbstractGatewayFilterFactory<Any>() {
    override fun apply(config: Any?): GatewayFilter {
        return GatewayFilter { exchange, _ ->
            val buffer = exchange.response.bufferFactory().wrap("true".toByteArray())

            exchange.response.statusCode = HttpStatus.OK
            exchange.response.headers.contentType = MediaType.APPLICATION_JSON
            exchange.response.writeWith(Flux.just(buffer))
        }
    }
}
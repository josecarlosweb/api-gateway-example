package com.ccarvalho.apigateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux


@Component
class NoAnswerFilter : AbstractGatewayFilterFactory<Any>() {
    override fun apply(config: Any?): GatewayFilter {
        return GatewayFilter { exchange, _ ->
            val buffer = exchange.response.bufferFactory().wrap("No answer".toByteArray())

            exchange.response.statusCode = HttpStatus.OK
            exchange.response.headers.contentType =  MediaType.TEXT_PLAIN
            exchange.response.writeWith(Flux.just(buffer))
        }
    }
}
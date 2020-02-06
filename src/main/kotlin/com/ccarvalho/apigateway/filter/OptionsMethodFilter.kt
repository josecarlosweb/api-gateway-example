package com.ccarvalho.apigateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class OptionsMethodFilter : AbstractGatewayFilterFactory<Any>() {
    override fun apply(config: Any?): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request

            if (request.method == HttpMethod.OPTIONS) {
                exchange.response.statusCode = HttpStatus.METHOD_NOT_ALLOWED
                exchange.response.setComplete()
            } else {
                chain.filter(exchange)
            }
        }
    }
}
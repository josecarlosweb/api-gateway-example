package com.ccarvalho.apigateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class SecureCookieFilter : AbstractGatewayFilterFactory<Any?>() {

    override fun apply(config: Any?): GatewayFilter {
        return GatewayFilter { exchange, chain -> chain.filter(exchange).then(Mono.fromRunnable<Void> { rewriteHeaders(exchange) }) }
    }

    private fun rewriteHeaders(exchange: ServerWebExchange) {
        val name = "Set-Cookie"
        val values = exchange.response.headers[name] ?: listOf()
        if (values.isNotEmpty()) {

            val newValues = values.map {
                var newValue: String = it
                if (!it.contains("secure", ignoreCase = true)) {
                    newValue = "$newValue; Secure"
                }
                if (!it.contains("HttpOnly", ignoreCase = true)) {
                    newValue = "$newValue; HttpOnly"
                }
                newValue
            }

            exchange.response.headers.remove(name)
            exchange.response.headers.addAll(name, newValues)

        }

    }
}
package com.ccarvalho.apigateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.stereotype.Component

@Component
class DoubleSlashFilter : AbstractGatewayFilterFactory<Any>() {
    override fun apply(config: Any?): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val noDoubleSlashes = exchange.request.uri.rawPath.replace("""//""", "/")
            chain.filter(exchange.mutate().request(exchange.request.mutate().path(noDoubleSlashes).build()).build())
        }
    }
}
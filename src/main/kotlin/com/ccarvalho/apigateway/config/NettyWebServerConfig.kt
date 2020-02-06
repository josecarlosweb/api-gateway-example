package com.ccarvalho.apigateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import reactor.netty.http.server.HttpRequestDecoderSpec
import reactor.netty.http.server.HttpServer


@Configuration
class NettyWebServerConfig constructor(
        @Value("\${server.max-http-header-size:8000000}") private val maxHttpHeaderSize: Int
        , @Value("\${server.max-initial-line-length:8000000}") private val maxHttpInitialLineLength: Int
) : WebServerFactoryCustomizer<NettyReactiveWebServerFactory>, Ordered {

    override fun getOrder(): Int {
        return Int.MAX_VALUE
    }

    override fun customize(factory: NettyReactiveWebServerFactory?) {
        factory!!.addServerCustomizers(NettyServerCustomizer { httpServer: HttpServer ->
            httpServer
                    .httpRequestDecoder { httpRequestDecoderSpec: HttpRequestDecoderSpec ->
                        httpRequestDecoderSpec
                                .maxHeaderSize(maxHttpHeaderSize)
                                .maxInitialLineLength(maxHttpInitialLineLength)
                    }
        })
    }

}
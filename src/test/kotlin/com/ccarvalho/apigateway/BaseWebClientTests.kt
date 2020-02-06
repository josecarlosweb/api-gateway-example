package com.ccarvalho.apigateway

import com.netflix.loadbalancer.Server
import com.netflix.loadbalancer.ServerList
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.route.Route
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_HANDLER_MAPPER_ATTR
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR
import org.springframework.cloud.netflix.ribbon.RibbonClient
import org.springframework.cloud.netflix.ribbon.StaticServerList
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration


open class BaseWebClientTests {

    @LocalServerPort
    protected var port = 0

    protected lateinit var testClient: WebTestClient

    protected lateinit var webClient: WebClient

    protected lateinit var baseUri: String

    @BeforeEach
    fun setup() {
        setup(ReactorClientHttpConnector(), "http://localhost:$port")
    }

    private fun setup(httpConnector: ClientHttpConnector, baseUri: String) {
        this.baseUri = baseUri
        this.webClient = WebClient.builder().clientConnector(httpConnector)
                .baseUrl(this.baseUri).build()
        this.testClient = WebTestClient.bindToServer(httpConnector).baseUrl(this.baseUri)
                .responseTimeout(Duration.ofMinutes(5))
                .build()
    }

    @Configuration
    @RibbonClient(name = "testservice", configuration = [TestRibbonConfig::class])
    class DefaultTestConfig {

        @Bean
        fun testController(): TestController {
            return TestController()
        }

        @Bean
        @Order(500)
        fun modifyResponseFilter(): GlobalFilter {
            return GlobalFilter { exchange, chain ->
                val value = exchange.getAttributeOrDefault(GATEWAY_HANDLER_MAPPER_ATTR,
                        "N/A")
                exchange.response.headers.add(HANDLER_MAPPER_HEADER, value)
                val route: Route? = exchange.getAttribute(GATEWAY_ROUTE_ATTR)
                if (route != null) {
                    exchange.response.headers.add(ROUTE_ID_HEADER,
                            route.id)
                }
                chain.filter(exchange)
            }
        }

    }

    @EnableAutoConfiguration
    @SpringBootConfiguration
    @Import(DefaultTestConfig::class)
    class MainConfig

    protected class TestRibbonConfig {

        @LocalServerPort
        private var port = 0

        @Bean
        fun ribbonServerList(): ServerList<Server> {
            return StaticServerList(Server("localhost", this.port))
        }

    }

    companion object {

        @JvmStatic
        protected val HANDLER_MAPPER_HEADER = "X-Gateway-Handler-Mapper-Class"

        @JvmStatic
        protected val ROUTE_ID_HEADER = "X-Gateway-RouteDefinition-Id"

    }

}

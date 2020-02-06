package com.ccarvalho.apigateway

import com.ccarvalho.apigateway.config.APIGatewayProperties
import com.ccarvalho.apigateway.discovery.DiscoveryClientMultiRouteDefinitionLocator
import com.ccarvalho.apigateway.filter.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.cloud.client.DefaultServiceInstance
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.gateway.discovery.GatewayDiscoveryClientAutoConfiguration
import org.springframework.cloud.gateway.filter.headers.ForwardedHeadersFilter
import org.springframework.cloud.gateway.filter.headers.XForwardedHeadersFilter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.expectBody


@ExtendWith(SpringExtension::class)
@DirtiesContext
@Import(BaseWebClientTests.DefaultTestConfig::class)
@SpringBootTest(
        classes = [
            ApiGatewayApplicationTests.Config::class,
            DiscoveryClientMultiRouteDefinitionLocator::class,
            DoubleSlashFilter::class,
            OptionsMethodFilter::class,
            APIGatewayProperties::class,
            SecureCookieFilter::class,
            BooleanAnswerFilter::class,
            NoAnswerFilter::class],
        webEnvironment = RANDOM_PORT)
class ApiGatewayApplicationTests : BaseWebClientTests(){

    @Autowired
    private lateinit var routeLocator: RouteLocator

    @Test
    fun rightNumberOfRoutes() {
        val routes = routeLocator.routes.collectList().block()

        assertNotNull(routes)
        assertEquals(8,routes!!.size)
        assertEquals(3, routes.filter { route -> route.id.startsWith("CC_") }.size)
        assertEquals(2, routes.filter { route -> route.id.startsWith("ANALYTICS_") }.size)
        assertEquals(1, routes.filter { route -> route.id.startsWith("live_gateway") }.size)
        assertEquals(1, routes.filter { route -> route.id.startsWith("disable_device_statistic") }.size)
        assertEquals(1, routes.filter { route -> route.id.startsWith("remove_lai_header") }.size)

    }

    @Test
    fun testForwardedHeaders() {
        @Suppress("UNCHECKED_CAST")
        testClient.get().uri("/get").header("Host", "dev1.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectBody<MutableMap<String, Any>>().consumeWith {
                    val headers = it.responseBody?.get("headers") as Map<String, *>
                    assertThat(headers)
                            .containsKeys(
                                    ForwardedHeadersFilter.FORWARDED_HEADER,
                                    XForwardedHeadersFilter.X_FORWARDED_FOR_HEADER,
                                    XForwardedHeadersFilter.X_FORWARDED_HOST_HEADER,
                                    XForwardedHeadersFilter.X_FORWARDED_PORT_HEADER,
                                    XForwardedHeadersFilter.X_FORWARDED_PROTO_HEADER
                            )
                }
    }

    @Test
    fun testSecurityHeaders() {
        testClient.get().uri("/SessionCookie").header("Host", "dev1.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "CC_SOME-SERVER-DEV1")
                .expectHeader().valueEquals("Set-Cookie",
                        "SESSION=abc-123-def; path=/; HttpOnly; Secure",
                        "metabase.SESSION=cdf-123-def; path=/; Secure; HttpOnly")

                .expectHeader().valueEquals("Public-Key-Pins",
                        "pin-sha256=\"shatest1\"; pin-sha256=\"shatest2\"; max-age=2592000; includeSubDomains")

        testClient.get().uri("/").header("Host", "dev2.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "live_gateway")
                .expectHeader().valueEquals("Public-Key-Pins",
                        "pin-sha256=\"shatest1\"; pin-sha256=\"shatest2\"; max-age=2592000; includeSubDomains")

        testClient.get().uri("/analytics").header("Host", "dev3.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "ANALYTICS_GM-ANALYTICS-DEV3")
                .expectHeader().valueEquals("Public-Key-Pins",
                        "pin-sha256=\"shatest1\"; pin-sha256=\"shatest2\"; max-age=2592000; includeSubDomains")
    }

    @Test
    fun testGMSRoute() {
        testClient.get().uri("/ServerVersion").header("Host", "dev1.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "CC_SOME-SERVER-DEV1")
        testClient.get().uri("/ServerVersion").header("Host", "dev2.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "CC_SOME-SERVER-DEV2")
        testClient.get().uri("/ServerVersion").header("Host", "dev3.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "CC_SOME-SERVER-DEV3")
    }

    @Test
    fun testSpaceEncoding() {
        testClient.get().uri("/Route/1244792/Stop/SHOPPING IGUATEMI FORTALEZA/Arrive/Driver?id=34775")
                .header("Host", "dev1.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "CC_SOME-SERVER-DEV1")
                .expectBody<MutableMap<String, Any>>().consumeWith {
                    assertThat(it.responseBody?.get("stopKey")).isEqualTo("SHOPPING IGUATEMI FORTALEZA")
                }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testPreserveHostHeader() {
        testClient.get().uri("/ServerVersion").header("Host", "dev1.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectBody<MutableMap<String, Any>>().consumeWith {
                    val headers = it.responseBody?.get("headers") as Map<String, *>
                    assertThat(headers["Host"]).isEqualTo("dev1.ccarvalho.com")
                }
    }

    @Test
    fun testAnalyticsRoute() {
        testClient.get().uri("/analytics/").header("Host", "dev2.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "ANALYTICS_GM-ANALYTICS-DEV2")
        testClient.get().uri("/analytics").header("Host", "dev2.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "ANALYTICS_GM-ANALYTICS-DEV2")
        testClient.get().uri("/analytics/testpath/").header("Host", "dev2.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "ANALYTICS_GM-ANALYTICS-DEV2")
                .expectBody<MutableMap<String, Any>>().consumeWith {
                    assertThat(it.responseBody?.get("subpath")).isEqualTo("testpath")
                }
    }

    @Test
    fun testLiveGatewayRoute() {
        testClient.get().uri("/").header("Host", "dev1.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "live_gateway")
        testClient.get().uri("/index.html").header("Host", "dev2.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "live_gateway")
    }


    @Test
    fun testOptionsNotAllowed() {
        testClient.options().uri("/anything").header("Host", "dev1.ccarvalho.com").exchange()
                .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
                .expectHeader().doesNotExist(ROUTE_ID_HEADER)
                // The proxied server (in this case, the test controller) didn't get the request,
                // and the request is finished before getting to the post filters.
                // Because of that, there is no route id header. If there was, it would be CC_SOME-SERVER-DEV1.
    }

    @Test
    fun testRemoveHeaderActivityOnSaveLog(){
        val headerActivity = "ccarvalho-LAI"
        testClient.post()
                .uri("/DeviceStatistic/Log")
                .header("Host", "dev1.ccarvalho.com")
                .header(headerActivity, "123")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk
                .expectHeader().doesNotExist(headerActivity)
    }

    @Test
    fun testDisableDeviceStatistic(){
        val answer = "true".toByteArray()
        testClient.post().uri("/DeviceStatistic").header("Host", "dev3.ccarvalho.com")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody().consumeWith {  result -> assertThat(result.responseBody).isEqualTo(answer)}

        testClient.post().uri("/DeviceStatistic/Log").header("Host", "dev3.ccarvalho.com")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody().consumeWith {  result -> assertThat(result.responseBody).isEqualTo(answer)}


        // It doesn't matter that there is no "dev4" registered to Eureka. This route always matches all listed hosts.
        // Even if they're down, the API Gateway answers for them.
        testClient.post().uri("/DeviceStatistic").header("Host", "dev4.ccarvalho.com")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody().consumeWith {  result -> assertThat(result.responseBody).isEqualTo(answer)}

        // If the host is not listed in the host filter, but is registered to Eureka, GMS will answer the request normally.
        testClient.post().uri("/DeviceStatistic").header("Host", "dev2.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "CC_SOME-SERVER-DEV2")

        // If the host is not listed in the host filter, but is also not registered to Eureka, we get the usual NOT FOUND response.
        testClient.post().uri("/DeviceStatistic").header("Host", "notregistered.ccarvalho.com").exchange()
                .expectStatus().isNotFound

    }

    @Test
    fun servicesNotRegistered() {
        testClient.get().uri("/ServerVersion").header("Host", "notregistered.ccarvalho.com").exchange()
                .expectStatus().isNotFound

        // There is no analytics registered for dev1, so GMS answers the request
        testClient.get().uri("/analytics/").header("Host", "dev1.ccarvalho.com").exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "CC_SOME-SERVER-DEV1")

        testClient.get().uri("/").header("Host", "server.notccarvalho.com").exchange()
                .expectStatus().isNotFound

        // The live-gateway route always matches hosts with ".ccarvalho.com", even if there are no server instances registered.
        // In the real world, the live-gateway service should make a request to doesnotexist.ccarvalho.com/ServerVersion
        // and verify that it is down, then show the "Server Down" page.
        // The test does not simulate this behavior, as seen below.
        testClient.get().uri("/").header("Host", "notregistered.ccarvalho.com").exchange()
                .expectHeader().valueEquals(ROUTE_ID_HEADER, "live_gateway")
                .expectStatus().isOk
    }


    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = [GatewayDiscoveryClientAutoConfiguration::class])
    internal class Config {
        @Bean
        internal fun discoveryClient(): TestDiscoveryClient {
            return TestDiscoveryClient()
        }
    }


    class TestDiscoveryClient : DiscoveryClient {

        private var serviceInstances = mapOf(
                "SOME-SERVER-DEV1" to DefaultServiceInstance("DEV1_1",
                        "SOME-SERVER-DEV1", "localhost", 8001, false),
                "SOME-SERVER-DEV2" to DefaultServiceInstance("DEV2_1",
                        "SOME-SERVER-DEV2", "localhost", 8002, false),
                "SOME-SERVER-DEV3" to DefaultServiceInstance("DEV3_1",
                        "SOME-SERVER-DEV3", "localhost", 8003, false),
                "GM-LIVE-GATEWAY-TEST" to DefaultServiceInstance("LGW_1",
                        "GM-LIVE-GATEWAY-TEST", "localhost", 8003, false),
                "GM-ANALYTICS-DEV2" to DefaultServiceInstance("ANA_DEV2_1",
                        "GM-ANALYTICS-DEV2", "localhost", 8004, false),
                "GM-ANALYTICS-DEV3" to DefaultServiceInstance("ANA_DEV3_1",
                        "GM-ANALYTICS-DEV3", "localhost", 8005, false)
        )

        override fun getServices(): MutableList<String> {
            return serviceInstances.keys.toMutableList()
        }

        override fun description(): String? {
            return null
        }

        override fun getInstances(serviceId: String): List<ServiceInstance> {
            return listOfNotNull(serviceInstances[serviceId])
        }
    }
}

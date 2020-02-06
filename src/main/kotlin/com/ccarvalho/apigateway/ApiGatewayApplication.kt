package com.ccarvalho.apigateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.gateway.discovery.GatewayDiscoveryClientAutoConfiguration

@EnableDiscoveryClient
@SpringBootApplication(exclude = [GatewayDiscoveryClientAutoConfiguration::class])
class ApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}

package com.ccarvalho.apigateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("gm.api-gateway.discovery")
class APIGatewayProperties {

    var locators: List<DiscoveryLocatorProperties> = listOf()

}
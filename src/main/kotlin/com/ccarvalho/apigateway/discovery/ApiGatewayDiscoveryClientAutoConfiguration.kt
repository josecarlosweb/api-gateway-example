package com.ccarvalho.apigateway.discovery

import com.ccarvalho.apigateway.config.APIGatewayProperties
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientAutoConfiguration
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.DispatcherHandler

@Configuration
@ConditionalOnProperty(name = ["spring.cloud.gateway.enabled"], matchIfMissing = true)
@AutoConfigureBefore(GatewayAutoConfiguration::class)
@AutoConfigureAfter(CompositeDiscoveryClientAutoConfiguration::class)
@ConditionalOnClass(DispatcherHandler::class, DiscoveryClient::class)
@EnableConfigurationProperties
class ApiGatewayDiscoveryClientAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = ["spring.cloud.gateway.discovery.locator.enabled"])
    fun discoveryClientRouteDefinitionLocator(
            discoveryClient: DiscoveryClient, properties: APIGatewayProperties): DiscoveryClientMultiRouteDefinitionLocator {
        return DiscoveryClientMultiRouteDefinitionLocator(discoveryClient, properties)
    }

}


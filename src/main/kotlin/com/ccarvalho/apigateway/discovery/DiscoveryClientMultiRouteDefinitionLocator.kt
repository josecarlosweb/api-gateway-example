package com.ccarvalho.apigateway.discovery

import ch.qos.logback.core.boolex.EvaluationException
import com.ccarvalho.apigateway.config.APIGatewayProperties
import org.apache.commons.logging.LogFactory
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties
import org.springframework.cloud.gateway.filter.FilterDefinition
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition
import org.springframework.cloud.gateway.route.RouteDefinition
import org.springframework.cloud.gateway.route.RouteDefinitionLocator
import org.springframework.core.style.ToStringCreator
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.SimpleEvaluationContext
import org.springframework.util.StringUtils
import reactor.core.publisher.Flux
import java.net.URI
import java.text.ParseException
import java.util.function.Predicate


class DiscoveryClientMultiRouteDefinitionLocator(private val discoveryClient: DiscoveryClient,
                                            private val properties: APIGatewayProperties) : RouteDefinitionLocator {

    private val evalCtxt: SimpleEvaluationContext = SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods()
            .build()

    private fun getRouteIdPrefix(properties: DiscoveryLocatorProperties) : String {
        return if (StringUtils.hasText(properties.routeIdPrefix)) {
            properties.routeIdPrefix!!
        } else {
            this.discoveryClient::class.java.simpleName + "_"
        }
    }

    private fun getEachRouteDefinitions(order: Int, properties: DiscoveryLocatorProperties): Flux<RouteDefinition> {
        val parser = SpelExpressionParser()
        val includeExpr = parser
                .parseExpression(properties.includeExpression)
        val urlExpr = parser.parseExpression(properties.urlExpression)

        val includePredicate: Predicate<ServiceInstance>
        if (properties.includeExpression == null || "true".equals(properties.includeExpression, ignoreCase = true)) {
            includePredicate = Predicate { true }
        } else {
            includePredicate = Predicate { instance: ServiceInstance ->
                val include = includeExpr.getValue(evalCtxt, instance, Boolean::class.java)

                include?: false
            }
        }

        return Flux.fromIterable(discoveryClient.services)
                .map(discoveryClient::getInstances)
                .filter { instances -> instances.isNotEmpty() }
                .map { instances -> instances[0] }.filter(includePredicate)
                .map { instance ->
                    val serviceId = instance.serviceId

                    val routeDefinition = RouteDefinition()
                    routeDefinition.id = getRouteIdPrefix(properties) + serviceId
                    val uri = urlExpr.getValue(evalCtxt, instance, String::class.java)
                    routeDefinition.uri = URI.create(uri)

                    val instanceForEval = DelegatingServiceInstance(
                            instance, properties)

                    for (original in properties.predicates) {
                        val predicate = PredicateDefinition()
                        predicate.name = original.name
                        for (entry in original.args
                                .entries) {
                            val value = getValueFromExpr(evalCtxt, parser,
                                    instanceForEval, entry)
                            predicate.addArg(entry.key, value)
                        }
                        routeDefinition.predicates.add(predicate)
                    }

                    for (original in properties.filters) {
                        val filter = FilterDefinition()
                        filter.name = original.name
                        for (entry in original.args
                                .entries) {
                            val value = getValueFromExpr(evalCtxt, parser,
                                    instanceForEval, entry)
                            filter.addArg(entry.key, value)
                        }
                        routeDefinition.filters.add(filter)
                    }
                    routeDefinition.order = order
                    routeDefinition
                }
    }
    override fun getRouteDefinitions(): Flux<RouteDefinition> {

        return Flux.concat(this.properties.locators.mapIndexed {order, locator -> getEachRouteDefinitions(order, locator) })
    }

    private fun getValueFromExpr(evalCtxt: SimpleEvaluationContext, parser: SpelExpressionParser,
                                 instance: ServiceInstance, entry: Map.Entry<String, String>): String {
        try {
            val valueExpr = parser.parseExpression(entry.value)
            return valueExpr.getValue(evalCtxt, instance, String::class.java)!!
        } catch (e: ParseException) {
            if (log.isDebugEnabled) {
                log.debug("Unable to parse " + entry.value, e)
            }
            throw e
        } catch (e: EvaluationException) {
            if (log.isDebugEnabled) {
                log.debug("Unable to parse " + entry.value, e)
            }
            throw e
        }

    }

    private class DelegatingServiceInstance constructor(internal val delegate: ServiceInstance,
                                                                private val properties: DiscoveryLocatorProperties) : ServiceInstance {

        override fun getServiceId(): String {
            return if (properties.isLowerCaseServiceId) {
                delegate.serviceId.toLowerCase()
            } else delegate.serviceId
        }

        override fun getHost(): String {
            return delegate.host
        }

        override fun getPort(): Int {
            return delegate.port
        }

        override fun isSecure(): Boolean {
            return delegate.isSecure
        }

        override fun getUri(): URI {
            return delegate.uri
        }

        override fun getMetadata(): Map<String, String> {
            return delegate.metadata
        }

        override fun getScheme(): String {
            return delegate.scheme
        }

        override fun toString(): String {
            return ToStringCreator(this).append("delegate", delegate)
                    .append("properties", properties).toString()
        }

    }

    companion object {

        private val log = LogFactory
                .getLog(DiscoveryClientMultiRouteDefinitionLocator::class.java)
    }

}
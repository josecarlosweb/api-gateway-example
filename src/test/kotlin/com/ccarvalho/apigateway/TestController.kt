package com.ccarvalho.apigateway

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping("/test")
class TestController {

    @RequestMapping("/")
    fun home(): String {
        return "Test root"
    }

    @RequestMapping(path = ["/management/prometheus"], method = [RequestMethod.GET, RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun prometheus() : String{
        return "Prometheus endpoint!"
    }

    @RequestMapping(path = ["/{anything:.+}"], method = [RequestMethod.GET, RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE] )
    fun headers(exchange: ServerWebExchange, @PathVariable(required = false) anything: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        result["headers"] = getHeaders(exchange)
        result["subpath"] = anything
        return result
    }

    @RequestMapping(path = ["/Route/{routeId}/Stop/{stopKey}/Arrive/Driver"], method = [RequestMethod.GET, RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun stopArrive(exchange: ServerWebExchange, @PathVariable(required = false) routeId: Int, @PathVariable(required = false) stopKey: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        result["headers"] = getHeaders(exchange)
        result["routeId"] = routeId
        result["stopKey"] = stopKey
        return result
    }

    @RequestMapping(path = ["/DeviceStatistic", "/DeviceStatistic/Log"], method = [RequestMethod.GET, RequestMethod.POST], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createDeviceStatisticLog(exchange: ServerWebExchange, @RequestBody(required = false) log: Map<String, Any>?): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        result["headers"] = getHeaders(exchange)
        if(!log.isNullOrEmpty()) {
            result["log"] = log
        }
        return result
    }

    @RequestMapping(path = ["/SessionCookie"], method = [RequestMethod.GET], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sessionCookie(exchange: ServerWebExchange): ResponseEntity<Map<String, Any>> {
        val result = mutableMapOf<String, Any>()
        result["headers"] = getHeaders(exchange)
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", "SESSION=abc-123-def; path=/; HttpOnly")
                .header("Set-Cookie", "metabase.SESSION=cdf-123-def; path=/")
                .body(result)
    }

    @RequestMapping("/status/{status}")
    fun status(@PathVariable status: Int): ResponseEntity<String> {
        return ResponseEntity.status(status).body("Failed with $status")
    }

    fun getHeaders(exchange: ServerWebExchange): Map<String, String> {
        return exchange.request.headers.toSingleValueMap()
    }

}
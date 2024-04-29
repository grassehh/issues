package com.grassehh

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.ContentType.*
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking
import org.zalando.logbook.Logbook
import org.zalando.logbook.client.LogbookClient
import org.zalando.logbook.common.ExperimentalLogbookKtorApi
import org.zalando.logbook.json.JsonHttpLogFormatter
import org.zalando.logbook.logstash.LogstashLogbackSink
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.Test
import kotlin.test.assertEquals

private const val DATA_LENGTH = 5_000

@WireMockTest
class ClientTest {

    @Test
    fun `should receiving large response body`(wireMockRuntimeInfo: WireMockRuntimeInfo): Unit = runBlocking {
        val largeResponseBody = """{"property": "${"a".repeat(DATA_LENGTH)}"}"""
        stubFor(
            post(urlEqualTo("/test"))
                .willReturn(aResponse().withBody(largeResponseBody).withStatus(200))
        )
        val client = buildClient(wireMockRuntimeInfo)
        assertEquals(
            largeResponseBody,
            client.post("test") { header(ContentType, Application.Json.toString()) }.body<String>()
        )
    }

    @Test
    fun `should send large request body`(wireMockRuntimeInfo: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            post(urlEqualTo("/test")).willReturn(aResponse().withBody("{}").withStatus(200))
        )
        val client = buildClient(wireMockRuntimeInfo)
        assertEquals(client.post("test") {
            setBody(TestData("a".repeat(DATA_LENGTH)))
            header(ContentType, Application.Json.toString())
        }.body<String>(), "{}")
    }

    @Test
    fun `should both send and receive large body`(wireMockRuntimeInfo: WireMockRuntimeInfo): Unit = runBlocking {
        val largeResponseBody = """{"property": "${"a".repeat(DATA_LENGTH)}"}"""
        stubFor(
            post(urlEqualTo("/test"))
                .willReturn(aResponse().withBody(largeResponseBody).withStatus(200))
        )
        val client = buildClient(wireMockRuntimeInfo)
        assertEquals(
            largeResponseBody,
            client.post("test") {
                setBody(TestData("a".repeat(DATA_LENGTH)))
                header(ContentType, Application.Json.toString())
            }.body<String>()
        )
    }

    @OptIn(ExperimentalLogbookKtorApi::class)
    private fun buildClient(wireMockRuntimeInfo: WireMockRuntimeInfo) = HttpClient(Java) {
        install(ContentNegotiation) {
            jackson()
        }
        install(LogbookClient) {
            logbook = Logbook.builder().sink(LogstashLogbackSink(JsonHttpLogFormatter())).build()
        }
//        install(com.grassehh.logbook.LogbookClient) {
//            logbook = Logbook.builder().sink(LogstashLogbackSink(JsonHttpLogFormatter())).build()
//        }
        install(HttpTimeout) {
            requestTimeoutMillis = SECONDS.toMillis(2)
            socketTimeoutMillis = SECONDS.toMillis(2)
            connectTimeoutMillis = SECONDS.toMillis(2)
        }
        defaultRequest {
            url("http://localhost:${wireMockRuntimeInfo.httpPort}/")
        }
    }

    private data class TestData(val property: String)
}
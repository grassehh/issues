package com.grassehh

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.ContentType.*
import io.ktor.http.HttpHeaders.ContentType
import kotlinx.coroutines.runBlocking
import org.zalando.logbook.Logbook
import org.zalando.logbook.client.LogbookClient
import org.zalando.logbook.common.ExperimentalLogbookKtorApi
import org.zalando.logbook.json.JsonHttpLogFormatter
import org.zalando.logbook.logstash.LogstashLogbackSink
import kotlin.test.Test

@WireMockTest
class ClientTest {

    @OptIn(ExperimentalLogbookKtorApi::class)
    @Test
    fun `should preserve Content-Type header`(wireMockRuntimeInfo: WireMockRuntimeInfo): Unit = runBlocking {
        stubFor(
            post(urlEqualTo("/test")).withHeader(ContentType, equalTo("text/plain; charset=UTF-8"))
                .withRequestBody(equalTo("Hello, world!"))
                .willReturn(aResponse().withStatus(200))
        )
        val client = HttpClient(Java) {
            install(LogbookClient) {
                logbook = Logbook.builder().sink(LogstashLogbackSink(JsonHttpLogFormatter())).build()
            }
            defaultRequest {
                url("http://localhost:${wireMockRuntimeInfo.httpPort}/")
            }
        }
        client.post("test") {
            setBody("Hello, world!")
            header(ContentType, Text.Plain.toString())
        }
        verify(
            postRequestedFor(urlEqualTo("/test")).withHeader("Content-Type", equalTo("text/plain; charset=UTF-8"))
                .withRequestBody(
                    equalTo("Hello, world!")
                )
        )
    }
}
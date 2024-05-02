package com.grassehh

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.zalando.logbook.Logbook
import org.zalando.logbook.common.ExperimentalLogbookKtorApi
import org.zalando.logbook.json.JsonHttpLogFormatter
import org.zalando.logbook.logstash.LogstashLogbackSink
import org.zalando.logbook.server.LogbookServer
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerTest {
    @OptIn(ExperimentalLogbookKtorApi::class)
    @Test
    fun testRoot() = testApplication {
        install(LogbookServer) {
            logbook = Logbook.builder()
                .sink(LogstashLogbackSink(JsonHttpLogFormatter()))
                .build()
        }
//        install(com.grassehh.logbook.LogbookServer) {
//            logbook = Logbook.builder()
//                .sink(LogstashLogbackSink(JsonHttpLogFormatter()))
//                .build()
//        }
        routing {
            post("/test") {
                call.receive<String>()
                call.respond(HttpStatusCode.OK)
            }
        }
        val response = client.post("/test") {
            headers { append(ContentType, "application/json") }
            setBody(
                """
                {
                    "name": "John Doe",
                    "age": 25
                }
            """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}


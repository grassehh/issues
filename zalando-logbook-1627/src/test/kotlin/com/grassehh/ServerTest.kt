package com.grassehh

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
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
            get("/test") {
                call.respond(HttpStatusCode.OK, "Hello world !")
            }
        }
        val response = client.get("/test")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello world !", response.bodyAsText())
    }
}


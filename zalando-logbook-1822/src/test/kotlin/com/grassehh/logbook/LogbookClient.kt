package com.grassehh.logbook

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.observer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.zalando.logbook.Logbook
import org.zalando.logbook.Logbook.ResponseProcessingStage
import org.zalando.logbook.common.readBytes

class LogbookClient(val logbook: Logbook) {
    class Config {
        var logbook: Logbook = Logbook.create()
    }

    companion object : HttpClientPlugin<Config, LogbookClient> {
        private val responseProcessingStageKey: AttributeKey<Deferred<ResponseProcessingStage>> =
            AttributeKey("Logbook.ResponseProcessingStage")
        override val key: AttributeKey<LogbookClient> = AttributeKey("LogbookPlugin")
        override fun prepare(block: Config.() -> Unit): LogbookClient = LogbookClient(Config().apply(block).logbook)

        @OptIn(InternalAPI::class)
        override fun install(plugin: LogbookClient, scope: HttpClient) {
            scope.sendPipeline.intercept(HttpSendPipeline.Monitoring) {
                val request = ClientRequest(context)
                context.attributes.put(responseProcessingStageKey, async(coroutineContext) {
                    val requestWritingStage = plugin.logbook.process(request)
                    if (request.shouldBuffer()) {
                        request.buffer((it as OutgoingContent).readBytes2(scope))
                    }
                    requestWritingStage.write()
                })
                proceedWith(it)
            }

            scope.receivePipeline.intercept(HttpReceivePipeline.After) { response ->
                val (loggingContent, responseContent) = response.content.split(response)
                scope.launch(coroutineContext) {
                    val responseProcessingStage = response.call.attributes[responseProcessingStageKey].await()
                    val clientResponse = ClientResponse(response)
                    val responseWritingStage = responseProcessingStage.process(clientResponse)
                    if (loggingContent.isClosedForRead.not()) {
                        if (clientResponse.shouldBuffer()) {
                            val content = loggingContent.readBytes()
                            clientResponse.buffer(content)
                        }
                        loggingContent.discard()
                    }
                    responseWritingStage.write()
                }
                proceedWith(response.call.wrapWithContent(responseContent).response)
            }
        }
    }
}

private suspend fun OutgoingContent.readBytes2(client: HttpClient): ByteArray = when (this) {
    is OutgoingContent.NoContent -> ByteArray(0)
    is OutgoingContent.ProtocolUpgrade -> ByteArray(0)
    is OutgoingContent.ByteArrayContent -> bytes()
    is OutgoingContent.ReadChannelContent -> readFrom().readBytes()
    is OutgoingContent.WriteChannelContent -> client.writer(Dispatchers.IO) { writeTo(channel) }.channel.readBytes()
}

package com.namirai.nikai.data.remote

import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class ChatApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun streamChat(
        endpoint: BackendEndpoint,
        payload: ChatRequestPayload,
        onEvent: (ChatStreamEvent) -> Unit,
    ): Unit = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url(endpoint.url(CHAT_PATH))
            .post(payload.toJson().toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val call = client.forEndpoint(endpoint).newCall(request)

        continuation.invokeOnCancellation {
            call.cancel()
        }

        call.enqueue(
            object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use {
                            if (!it.isSuccessful) {
                                throw IOException("NIK AI returned HTTP ${it.code}.")
                            }

                            when (
                                ChatResponseKind.fromContentType(
                                    it.header("Content-Type"),
                                )
                            ) {
                                ChatResponseKind.PlainText -> {
                                    onEvent(ChatStreamEvent.PlainText(it.body.string()))
                                }
                                ChatResponseKind.Ndjson -> {
                                    readNdjsonResponse(it, onEvent)
                                }
                                ChatResponseKind.Unsupported -> {
                                    throw ChatProtocolException(
                                        "NIK AI returned an unsupported response type.",
                                    )
                                }
                            }
                        }

                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    } catch (error: Exception) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }
                }
            },
        )
    }

    private fun readNdjsonResponse(
        response: Response,
        onEvent: (ChatStreamEvent) -> Unit,
    ) {
        val source = response.body.source()
        var terminalEventReceived = false
        var firstTokenReceived = false

        while (!terminalEventReceived) {
            val line = source.readUtf8Line() ?: break
            val event = ChatStreamEventParser.parseNdjsonLine(line) ?: continue
            if (event is ChatStreamEvent.Token && !firstTokenReceived) {
                firstTokenReceived = true
                Log.i(TAG, "First NDJSON token received.")
            }
            onEvent(event)
            terminalEventReceived =
                event is ChatStreamEvent.Done || event is ChatStreamEvent.Error
            if (event is ChatStreamEvent.Done) {
                Log.i(TAG, "NDJSON stream completed.")
            } else if (event is ChatStreamEvent.Error) {
                Log.w(TAG, "NDJSON stream returned an error event.")
            }
        }

        if (!terminalEventReceived) {
            throw ChatProtocolException("NIK AI stream ended before completion.")
        }
    }

    private companion object {
        const val TAG = "NIK-Chat"
        const val CHAT_PATH = "chat"
        const val CONNECT_TIMEOUT_SECONDS = 5L
        const val READ_TIMEOUT_SECONDS = 310L
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

package io.melbybaldove.gdaxticker

import io.melbybaldove.gdaxticker.model.*
import io.reactivex.FlowableEmitter
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * @author Melby Baldove
 * melby@appsolutely.ph
 */
class WebSocketEventRouter(private val emitter: FlowableEmitter<SocketEvent>) : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        if (!emitter.isCancelled) {
            emitter.onNext(SocketOpenEvent(webSocket, response))
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        if (!emitter.isCancelled) {
            emitter.onNext(SocketMessageEvent(text))
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {

    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        if (!emitter.isCancelled) {
            emitter.onNext(SocketClosingEvent(code, reason))
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        if (!emitter.isCancelled) {
            emitter.onNext(SocketClosedEvent(code, reason))
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        if (!emitter.isCancelled) {
            emitter.onNext(SocketFailedEvent(t, response))
        }
    }
}
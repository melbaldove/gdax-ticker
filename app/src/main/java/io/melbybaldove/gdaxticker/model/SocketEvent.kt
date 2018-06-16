package io.melbybaldove.gdaxticker.model

import okhttp3.Response
import okhttp3.WebSocket

/**
 * @author Melby Baldove
 * melby@appsolutely.ph
 */
sealed class SocketEvent

data class SocketOpenEvent(val ws: WebSocket, val response: Response): SocketEvent()

data class SocketMessageEvent(val message: String): SocketEvent()

data class SocketClosingEvent(val code: Int, val reason: String): SocketEvent()

data class SocketClosedEvent(val code: Int, val reason: String): SocketEvent()

data class SocketFailedEvent(val throwable: Throwable, val response: Response?): SocketEvent()
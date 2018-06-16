package io.melbybaldove.gdaxticker

import android.util.Log
import io.melbybaldove.gdaxticker.model.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.util.concurrent.TimeUnit


/**
 * @author Melby Baldove
 * melby@appsolutely.ph
 */
class RxWebSockets(private val url: String) {
    private val TAG = "RxWebSockets"
    private var webSocket: WebSocket? = null
    private var isConnectionAlive: Boolean = false
    private var socketEventProcessor = PublishProcessor.create<SocketEvent>()
    private var disposables = CompositeDisposable()
    private var connectionDisposables: CompositeDisposable? = null
    private val client: OkHttpClient = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    private val request: Request = Request.Builder()
            .url(url)
            .build()

    private fun getEventSource(): Flowable<SocketEvent> {
        return socketEventProcessor.onErrorResumeNext({ throwable: Throwable ->
            Log.e(TAG, "RxWebSocket EventSubject internal error occured.")
            Log.e(TAG, throwable.message)
            throwable.printStackTrace()
            PublishProcessor.create<SocketEvent>().apply {
                socketEventProcessor = this
            }
        })
    }

    @Synchronized
    fun connect() {
        connectionDisposables = CompositeDisposable()
        val webSocketInstanceDisposable = getEventSource()
                .ofType(SocketOpenEvent::class.java)
                .firstElement()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe({
                    webSocket = it.ws
                    isConnectionAlive = true
                }, {
                    Log.e(TAG, it.message)
                    it.printStackTrace()
                })
        val connectionDisposable = Flowable.create<SocketEvent>({ emitter ->
            client.newWebSocket(request, WebSocketEventRouter(emitter))
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe({ event ->
                    socketEventProcessor.onNext(event)
                }, {
                    Log.e(TAG, it.message)
                    it.printStackTrace()
                })
        connectionDisposables!!.apply {
            add(webSocketInstanceDisposable)
            add(connectionDisposable)
        }
        disposables.add(connectionDisposables!!)
    }

    fun send(message: String): Single<Boolean> = Single.fromCallable {
        webSocket?.send(message)
    }

    fun onOpen(): Flowable<SocketOpenEvent> = getEventSource().ofType(SocketOpenEvent::class.java)

    fun onMessage(): Flowable<SocketMessageEvent> = getEventSource().ofType(SocketMessageEvent::class.java)

    fun onClosing(): Flowable<SocketClosingEvent> = getEventSource().ofType(SocketClosingEvent::class.java)

    fun onClosed(): Flowable<SocketClosedEvent> = getEventSource().ofType(SocketClosedEvent::class.java)

    fun onFailure(): Flowable<SocketFailedEvent> = getEventSource().ofType(SocketFailedEvent::class.java)

}
package io.melbybaldove.gdaxticker

import io.reactivex.Single
import retrofit2.http.GET

/**
 * @author Melby Baldove
 * melby@appsolutely.ph
 */
interface GdaxApi {
    companion object {
        const val BASE_URL = "https://api.gdax.com/"
    }

    @GET("products")
    fun fetchTradingPairs(): Single<List<TradingPair>>
}
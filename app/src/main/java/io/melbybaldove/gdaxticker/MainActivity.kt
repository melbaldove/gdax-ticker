package io.melbybaldove.gdaxticker

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var gdaxApi: GdaxApi
    private lateinit var rxWebSockets: RxWebSockets

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initEverythingHereNowBecauseYes()
        activity_main_tradingPairs.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                unsubscribeFromAllChannels()
                subscribeToTicker(activity_main_tradingPairs.getItemAtPosition(position).toString())
            }

        }
        loadTradingPairs()
    }

    private fun loadTradingPairs() {
        gdaxApi.fetchTradingPairs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    rxWebSockets.connect()
                    val adapter = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_item, it.map { it.id })
                    activity_main_tradingPairs.adapter = adapter
                }, {
                    Log.d("errors", it.localizedMessage)
                    it.printStackTrace()
                })
    }

    private fun initEverythingHereNowBecauseYes() {
        gdaxApi = buildRetrofitApiClient()
        rxWebSockets = RxWebSockets("wss://ws-feed.gdax.com").apply {
            onOpen().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        subscribeToTicker(activity_main_tradingPairs.getItemAtPosition(activity_main_tradingPairs.selectedItemPosition).toString())
                    })
            onMessage().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
                val jsonObject = JsonParser().parse(it.message).asJsonObject
                when (jsonObject.get("type").asString) {
                    "ticker" -> {
                        val tradingPair = jsonObject.get("product_id").asString
                        activity_main_tradingPair.text = tradingPair
                        activity_main_tickValue.text = jsonObject.get("price").asString

                    }
                }
            }, {

            })
            onFailure().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
                it.throwable.printStackTrace()
                Log.d("rxWebSockets", it.throwable.localizedMessage)
            }, {

            })
        }
    }

    private fun buildRetrofitApiClient(): GdaxApi {
        val okHttpClient = OkHttpClient.Builder().build()
        val retrofitBuilder = Retrofit.Builder()
                .baseUrl(GdaxApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .client(okHttpClient)
                .build()
        return retrofitBuilder.create(GdaxApi::class.java)
    }

    private fun subscribeToTicker(tradingPair: String) {
        val subscriptionRequest = JsonObject().apply {
            addProperty("type", "subscribe")
            add("channels", Gson().toJsonTree(listOf("ticker")))
            add("product_ids", Gson().toJsonTree(listOf(tradingPair)))
        }
        rxWebSockets.send(subscriptionRequest.toString()).subscribe({},{})
    }

    private fun unsubscribeFromAllChannels() {
        val unsubscriptionRequest = JsonObject().apply {
            addProperty("type", "unsubscribe")
            add("channels", Gson().toJsonTree(listOf("ticker")))
        }
        rxWebSockets.send(unsubscriptionRequest.toString()).subscribe({},{})
    }
}

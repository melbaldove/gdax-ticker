package io.melbybaldove.gdaxticker

import com.google.gson.annotations.SerializedName

data class TradingPair(val id: String,
                       @SerializedName("display_name") val displayName: String,
                       val status: String)

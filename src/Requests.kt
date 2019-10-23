package dev.anli.ftskit

internal open class FTSRequest(val header: String)
internal data class FTSLoginRequest(val casename: String, val loginName: String, val pwd: String): FTSRequest("login")
internal data class FTSSubmitOrderRequest(val ticker: String, val tradeChoice: String, val qty: String): FTSRequest("submitOrder")
internal data class FTSGetTickersRequest(val startT: Int): FTSRequest("getTickers")
internal object FTSLogoutRequest: FTSRequest("logout")
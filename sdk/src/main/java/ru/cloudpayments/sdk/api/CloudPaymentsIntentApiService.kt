package ru.cloudpayments.sdk.api

import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import ru.cloudpayments.sdk.api.models.CloudpaymentsBinInfoResponse
import ru.cloudpayments.sdk.api.models.intent.CPGetIntentStatusResponse
import ru.cloudpayments.sdk.api.models.intent.CPPatchIntentItemRequestBody
import ru.cloudpayments.sdk.api.models.intent.CPPostIntentPayRequestBody
import ru.cloudpayments.sdk.api.models.intent.CPPostIntentPayResponse
import ru.cloudpayments.sdk.api.models.intent.CPPostIntentRequestBody
import ru.cloudpayments.sdk.api.models.intent.CPPostIntentResponse

interface CloudPaymentsIntentApiService {
	@POST("api/intent")
	fun postIntent(@Body body: CPPostIntentRequestBody): Single<Response<CPPostIntentResponse>>

	@PATCH("api/intent/{id}")
	fun patchIntent(@Header("secret") secret: String, @Path("id") intentId: String, @Body body: ArrayList<CPPatchIntentItemRequestBody>): Single<Response<CPPostIntentResponse>>

	@POST("api/intent/pay")
	fun postIntentPay(@Body body: CPPostIntentPayRequestBody): Single<Response<CPPostIntentPayResponse>>

	@GET("api/intent/alt/{id}/link/{paymentMethod}")
	fun getAltPayLink(@Path("id") intentId: String, @Path("paymentMethod") paymentMethod: String, @Query("puid") puid: String, @Query("schema") schema: String? = null, @Query("webView") webView: Boolean = false): Single<Response<String>>

	@GET("api/intent/{intentId}/status")
	fun getIntentStatus(@Path("intentId") intentId: String): Single<Response<CPGetIntentStatusResponse>>

	@GET("api/intent/{id}/mirPay/deeplink")
	fun getMirPayDeepLink(@Path("id") intentId: String, @Query("puid") puid: String, @Query("webView") webView: Boolean = false): Single<Response<String>>

	@GET("bins/info/{firstSixDigits}")
	fun getBinInfo(@Path("firstSixDigits") firstSixDigits: String): Single<CloudpaymentsBinInfoResponse>

	@GET("bins/info")
	fun getBinInfo(@QueryMap queryMap: Map<String, String>): Single<CloudpaymentsBinInfoResponse>
}
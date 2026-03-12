package ru.cloudpayments.sdk.api

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap
import ru.cloudpayments.sdk.api.models.CloudpaymentsBinInfoResponse
import ru.cloudpayments.sdk.api.models.CloudpaymentsPublicKeyResponse

interface CloudpaymentsApiService {

	@GET("bins/info/{firstSixDigits}")
	fun getBinInfo(@Path("firstSixDigits") firstSixDigits: String): Single<CloudpaymentsBinInfoResponse>

	@GET("bins/info")
	fun getBinInfo(@QueryMap queryMap: Map<String, String>): Single<CloudpaymentsBinInfoResponse>

	@GET("payments/publickey")
	fun getPublicKey(): Single<CloudpaymentsPublicKeyResponse>
}
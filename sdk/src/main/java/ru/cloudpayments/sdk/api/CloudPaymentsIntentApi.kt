package ru.cloudpayments.sdk.api

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import retrofit2.Response
import ru.cloudpayments.sdk.api.models.CloudpaymentsBinInfo
import ru.cloudpayments.sdk.api.models.CloudpaymentsBinInfoResponse
import ru.cloudpayments.sdk.api.models.CloudpaymentsTransactionError
import ru.cloudpayments.sdk.api.models.intent.CPGetIntentStatusResponse
import ru.cloudpayments.sdk.api.models.intent.CPPatchIntentItemRequestBody
import ru.cloudpayments.sdk.api.models.intent.CPPostIntentPayRequestBody
import ru.cloudpayments.sdk.api.models.intent.CPPostIntentPayResponse
import ru.cloudpayments.sdk.api.models.intent.CPPostIntentRequestBody
import ru.cloudpayments.sdk.api.models.intent.CPPostIntentResponse
import javax.inject.Inject

class CloudPaymentsIntentApi @Inject constructor(private val apiService: CloudPaymentsIntentApiService) {

	fun postIntent(requestBody: CPPostIntentRequestBody): Single<Response<CPPostIntentResponse>> {
		return apiService.postIntent(requestBody)
			.subscribeOn(Schedulers.io())
	}

	fun patchIntent(secret: String, intentId: String, requestBody: ArrayList<CPPatchIntentItemRequestBody>): Single<Response<CPPostIntentResponse>> {
		return apiService.patchIntent(secret, intentId, requestBody)
			.subscribeOn(Schedulers.io())
	}

	fun postIntentPay(requestBody: CPPostIntentPayRequestBody): Single<Response<CPPostIntentPayResponse>> {
		return apiService.postIntentPay(requestBody)
			.subscribeOn(Schedulers.io())
	}

	fun getAltPayLink(intentId: String, paymentMethod: String, puid: String): Single<Response<String>> {
		return apiService.getAltPayLink(intentId, paymentMethod, puid)
			.subscribeOn(Schedulers.io())
	}

	fun getAltPayLink(intentId: String, paymentMethod: String, puid: String, schema: String): Single<Response<String>> {
		return apiService.getAltPayLink(intentId, paymentMethod, puid, schema)
			.subscribeOn(Schedulers.io())
	}

	fun getIntentStatus(intentId: String): Single<Response<CPGetIntentStatusResponse>> {
		return apiService.getIntentStatus(intentId)
			.subscribeOn(Schedulers.io())
	}

	fun getMirPayDeepLink(intentId: String, puid: String): Single<Response<String>> {
		return apiService.getMirPayDeepLink(intentId, puid)
			.subscribeOn(Schedulers.io())
	}

	fun getBinInfo(firstSixDigits: String): Single<CloudpaymentsBinInfo> =
		if (firstSixDigits.length < 6) {
			Single.error(CloudpaymentsTransactionError("You must specify the first 6 digits of the card number"))
		} else {
			val firstSix = firstSixDigits.subSequence(0, 6).toString()
			apiService.getBinInfo(firstSix)
					.subscribeOn(Schedulers.io())
					.map { it.binInfo ?: CloudpaymentsBinInfo("", "", "", "", "",false) }
					.onErrorReturn { CloudpaymentsBinInfo("", "", "", "", "",false) }
		}

	fun getBinInfo(queryMap: Map<String, String>): Single<CloudpaymentsBinInfoResponse> {
		return apiService.getBinInfo(queryMap)
			.subscribeOn(Schedulers.io())
	}
}
package ru.cloudpayments.sdk.api

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import ru.cloudpayments.sdk.api.models.CloudpaymentsBinInfo
import ru.cloudpayments.sdk.api.models.CloudpaymentsBinInfoResponse
import ru.cloudpayments.sdk.api.models.CloudpaymentsPublicKeyResponse
import ru.cloudpayments.sdk.api.models.CloudpaymentsTransactionError
import javax.inject.Inject

class CloudpaymentsApi @Inject constructor(private val apiService: CloudpaymentsApiService) {

	fun getPublicKey(): Single<CloudpaymentsPublicKeyResponse> {
		return apiService.getPublicKey()
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
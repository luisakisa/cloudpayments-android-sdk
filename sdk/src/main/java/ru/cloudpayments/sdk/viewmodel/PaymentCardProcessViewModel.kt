package ru.cloudpayments.sdk.viewmodel

import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import retrofit2.Response
import ru.cloudpayments.sdk.api.CloudPaymentsIntentApi
import ru.cloudpayments.sdk.api.CloudpaymentsApi
import ru.cloudpayments.sdk.api.models.intent.CPPostIntentPayRequestBody
import ru.cloudpayments.sdk.api.models.intent.CPPostIntentPayResponse
import ru.cloudpayments.sdk.api.models.intent.CPTransactionInResponses
import ru.cloudpayments.sdk.configuration.PaymentData
import ru.cloudpayments.sdk.models.ApiError
import ru.cloudpayments.sdk.ui.dialogs.PaymentCardProcessStatus
import javax.inject.Inject

internal class PaymentCardProcessViewModel(
	private val intentId: String,
	private val paymentData: PaymentData,
	private val cryptogram: String,
	private val saveCard: Boolean?

): BaseViewModel<PaymentCardProcessViewState>() {
	override var currentState = PaymentCardProcessViewState()
	override val viewState: MutableLiveData<PaymentCardProcessViewState> by lazy {
		MutableLiveData(currentState)
	}

	private var disposable: Disposable? = null

	@Inject
	lateinit var api: CloudpaymentsApi

	@Inject
	lateinit var intentApi: CloudPaymentsIntentApi


	fun pay() {

		val body = CPPostIntentPayRequestBody(intentId = intentId,
											  paymentMethod = CPPostIntentPayRequestBody.PAYMENT_METHOD_CARD,
											  cryptogram = cryptogram)


		if (saveCard != null) {
			body.saveCard = saveCard
		}

			disposable = intentApi.postIntentPay(body)
				.toObservable()
				.observeOn(AndroidSchedulers.mainThread())
				.map { response ->
					checkPayResponse(response)
				}
				.onErrorReturn {
					val state = currentState.copy(status = PaymentCardProcessStatus.Failed, reasonCode = ApiError.CODE_ERROR_CONNECTION)
					stateChanged(state)
				}
				.subscribe()

	}

	fun clearThreeDsData(){
		val state = currentState.copy(acsUrl = null, paReq = null)
		stateChanged(state)
	}

	private fun checkPayResponse(response: Response<CPPostIntentPayResponse>) {

		val state = if (response.code() == 200) {
			currentState.copy(
				transaction = response.body()?.transaction,
				status = PaymentCardProcessStatus.Succeeded
			)
		} else if (response.code() == 409) {
			currentState.copy(
				transaction = response.body()?.transaction,
				status = PaymentCardProcessStatus.AlreadyPaid
			)
		} else if (response.code() == 202) {

			val paReq = response.body()?.paReq
			val threeDsCallbackId = response.body()?.threeDsCallbackId
			val acsUrl = response.body()?.acsUrl

			currentState.copy(
				transaction = response.body()?.transaction,
				paReq = paReq,
				threeDsCallbackId = threeDsCallbackId,
				acsUrl = acsUrl
			)

		} else if (response.code() == 402) {

			currentState.copy(
				transaction = response.body()?.transaction,
				status = PaymentCardProcessStatus.Failed,
				reasonCode = response.body()?.transaction?.code
			)

		} else {
			currentState.copy(
				status = PaymentCardProcessStatus.Failed
			)
		}

		stateChanged(state)
	}

	private fun stateChanged(viewState: PaymentCardProcessViewState) {
		currentState = viewState.copy()
		this.viewState.apply {
			value = viewState
		}
	}

	override fun onCleared() {
		super.onCleared()

		disposable?.dispose()
	}
}

internal data class PaymentCardProcessViewState(
	val status: PaymentCardProcessStatus = PaymentCardProcessStatus.InProcess,
	val succeeded: Boolean = false,
	val transaction: CPTransactionInResponses? = null,
	val paReq: String? = null,
	val threeDsCallbackId: String? = null,
	val acsUrl: String? = null,
	val reasonCode: String? = null,
	val transactionId: Long? = null
): BaseViewState()
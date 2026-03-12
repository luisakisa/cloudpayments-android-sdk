package ru.cloudpayments.sdk.viewmodel

import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import ru.cloudpayments.sdk.api.CloudPaymentsIntentApi
import ru.cloudpayments.sdk.api.models.CloudpaymentsTransaction
import ru.cloudpayments.sdk.api.models.intent.CPPaymentMethod
import ru.cloudpayments.sdk.models.ApiError
import ru.cloudpayments.sdk.ui.dialogs.PaymentSBPStatus
import ru.cloudpayments.sdk.util.getUUID
import java.util.Timer
import javax.inject.Inject

internal class PaymentSBPViewModel(
	private val intentId: String
): BaseViewModel<PaymentSBPViewState>() {
	override var currentState = PaymentSBPViewState()
	override val viewState: MutableLiveData<PaymentSBPViewState> by lazy {
		MutableLiveData(currentState)
	}

	private var disposable: Disposable? = null

	var timer: Timer? = null

	@Inject
	lateinit var intentApi: CloudPaymentsIntentApi

	fun getSbpPayLink(schema: String) {

		val transactionUuid = getUUID()

		disposable = intentApi.getAltPayLink(intentId, CPPaymentMethod.SBP, transactionUuid, schema)
			.toObservable()
			.observeOn(AndroidSchedulers.mainThread())
			.map { response ->
				val state = if (response.code() == 200 && response.body() != null && response.body()!!.isNotEmpty()) {
					currentState.copy(status = PaymentSBPStatus.RunBankApp,
									  payUrl = response.body(),
									  transactionUuid = transactionUuid)
				} else if (response.code() == 409) {
					currentState.copy(status = PaymentSBPStatus.AlreadyPaid)
				} else {
					currentState.copy(status = PaymentSBPStatus.Failed)
				}
				stateChanged(state)
			}
			.onErrorReturn {
				val state = currentState.copy(status = PaymentSBPStatus.Failed, reasonCode = ApiError.CODE_ERROR_CONNECTION)
				stateChanged(state)
			}
			.subscribe()
	}

	private fun stateChanged(viewState: PaymentSBPViewState) {
		currentState = viewState.copy()
		this.viewState.apply {
			value = viewState
		}
	}

	override fun onCleared() {
		super.onCleared()
		timer?.cancel()
		timer = null
		disposable?.dispose()
	}
}

internal data class PaymentSBPViewState(
	val status: PaymentSBPStatus = PaymentSBPStatus.ListOfBanks,
	val succeeded: Boolean = false,
	val transaction: CloudpaymentsTransaction? = null,
	val errorMessage: String? = null,
	val reasonCode: String? = null,
	val payUrl: String? = null,
	val transactionId: Long? = null,
	val transactionUuid: String? = null

): BaseViewState()
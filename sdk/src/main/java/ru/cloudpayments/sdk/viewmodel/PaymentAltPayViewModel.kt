package ru.cloudpayments.sdk.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.gson.Gson
import ru.cloudpayments.sdk.api.CloudPaymentsIntentApi
import ru.cloudpayments.sdk.api.models.CloudpaymentsTransaction
import ru.cloudpayments.sdk.api.models.intent.CPGetIntentStatusResponse
import ru.cloudpayments.sdk.api.models.intent.CPTransactionInResponses
import ru.cloudpayments.sdk.api.worker.CheckIntentStatusWorker
import ru.cloudpayments.sdk.ui.dialogs.PaymentAltPayStatus
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class PaymentAltPayViewModel(
	private val application: Application,
	private val intentId: String,
	private val transactionUuid: String
) : BaseViewModel<PaymentAltPayViewState>() {
	override var currentState = PaymentAltPayViewState()
	override val viewState: MutableLiveData<PaymentAltPayViewState> by lazy {
		MutableLiveData(currentState)
	}

	@Inject
	lateinit var intentApi: CloudPaymentsIntentApi

	fun getIntentStatusWithTimer() {

		val data = Data.Builder()
			.putString(CheckIntentStatusWorker.ARG_INTENT_ID, intentId)
			.build()

		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()

		val checkStatusRequest = OneTimeWorkRequestBuilder<CheckIntentStatusWorker>()
			.setInputData(data)
			.setInitialDelay(3, TimeUnit.SECONDS)
			.setConstraints(constraints)
			.build()

		WorkManager.getInstance(application)
			.getWorkInfoByIdLiveData(checkStatusRequest.id)
			.observeForever { workInfo ->
				if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
					val json = workInfo.outputData.getString(CheckIntentStatusWorker.ARG_INTENT)
					if (json != null) {
						val intent = Gson().fromJson(json, CPGetIntentStatusResponse::class.java)
						checkIntentStatusResponse(intent)
					} else {
						val state = currentState.copy(status = PaymentAltPayStatus.Failed)
						stateChanged(state)
					}
				} else if (workInfo != null && workInfo.state == WorkInfo.State.FAILED){
					getIntentStatusWithTimer()
				}
			}

		WorkManager.getInstance(application).enqueue(checkStatusRequest)
	}

	private fun checkIntentStatusResponse(response: CPGetIntentStatusResponse) {

		val transactions = response.transactions

		transactions?.let {
			var currentTransaction: CPTransactionInResponses? = null
			for (transaction in transactions) {
				if (transaction.puid == transactionUuid) {
					currentTransaction = transaction
					break
				}
			}
			currentTransaction?.let {
				when (currentTransaction.status) {
					"Authorized", "Completed" -> {
						val state = currentState.copy(
							status = PaymentAltPayStatus.Succeeded,
							transactionId = currentTransaction.transactionId
						)
						stateChanged(state)
					}
					"Declined" -> {
						val state = currentState.copy(
							status = PaymentAltPayStatus.Failed,
							transactionId = currentTransaction.transactionId,
							reasonCode = currentTransaction.code
						)
						stateChanged(state)
					}
					else -> {
						getIntentStatusWithTimer()
					}
				}
			} ?: run {
				getIntentStatusWithTimer()
			}
		}
			?: run {
				val state = currentState.copy(status = PaymentAltPayStatus.Failed)
				stateChanged(state)
			}
	}

	fun cancelCheckIntentStatus() {
		WorkManager.getInstance(application).cancelAllWork()
	}

	private fun stateChanged(viewState: PaymentAltPayViewState) {
		currentState = viewState.copy()
		this.viewState.apply {
			value = viewState
		}
	}
}

internal data class PaymentAltPayViewState(
	val status: PaymentAltPayStatus = PaymentAltPayStatus.InProcess,
	val transactionId: Long? = null,
	val transaction: CloudpaymentsTransaction? = null,
	val errorMessage: String? = null,
	val reasonCode: String? = null
) : BaseViewState()
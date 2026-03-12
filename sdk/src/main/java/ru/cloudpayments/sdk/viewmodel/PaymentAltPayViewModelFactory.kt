package ru.cloudpayments.sdk.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal class PaymentAltPayViewModelFactory(
	private val application: Application,
	private val intentId: String,
	private val transactionUuid: String
): ViewModelProvider.Factory {

	@Suppress("UNCHECKED_CAST")
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		return PaymentAltPayViewModel(application,intentId, transactionUuid) as T
	}
}
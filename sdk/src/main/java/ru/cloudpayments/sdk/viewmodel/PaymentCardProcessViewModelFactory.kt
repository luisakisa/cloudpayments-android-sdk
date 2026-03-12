package ru.cloudpayments.sdk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.cloudpayments.sdk.configuration.PaymentData

internal class PaymentCardProcessViewModelFactory(
	private val intentId: String,
	private val paymentData: PaymentData,
	private val cryptogram: String,
	private val saveCard: Boolean?
): ViewModelProvider.Factory {

	@Suppress("UNCHECKED_CAST")
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		return PaymentCardProcessViewModel(intentId, paymentData, cryptogram, saveCard) as T
	}
}
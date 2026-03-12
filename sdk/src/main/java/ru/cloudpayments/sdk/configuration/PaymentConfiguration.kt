package ru.cloudpayments.sdk.configuration

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.cloudpayments.sdk.scanner.CardScanner

@Parcelize
data class PaymentConfiguration(val publicId: String,
								val paymentData: PaymentData,
								val scanner: CardScanner? = null,
								val emailBehavior: EmailBehavior = EmailBehavior.OPTIONAL,
								val useDualMessagePayment: Boolean = false,
								var paymentMethodSequence: ArrayList<String> = ArrayList(),
								val singlePaymentMode: String? = null,
								val showResultScreenForSinglePaymentMode: Boolean = true,
								val saveCardForSinglePaymentMode: Boolean? = null,
								val testMode: Boolean = false): Parcelable

enum class EmailBehavior {
	OPTIONAL,   // пользователь может включить/отключить
	REQUIRED,   // обязательно для заполнения
	HIDDEN      // поле не отображается
}


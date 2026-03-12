package ru.cloudpayments.sdk.configuration

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import ru.cloudpayments.sdk.api.models.PaymentDataPayer
import ru.cloudpayments.sdk.api.models.intent.CPRecurrent

@Parcelize
class PaymentData(
	val amount: String,
	var currency: String = "RUB",
	val externalId: String? = null,
	val description: String? = null,
	val accountId: String? = null,
	var email: String? = null,
	val payer: PaymentDataPayer? = null,
	val recurrent: CPRecurrent? = null,
	val receipt: Map<String, @RawValue Any?>? = null,
	val jsonData: String? = null
) : Parcelable



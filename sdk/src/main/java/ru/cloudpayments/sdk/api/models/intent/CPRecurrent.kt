package ru.cloudpayments.sdk.api.models.intent

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class CPRecurrent(
	@SerializedName("interval") var interval: String,
	@SerializedName("period") var period: Int,
	@SerializedName("maxPeriods") var maxPeriods: Int? = null,
	@SerializedName("startDate") var startDate: String? = null,
	@SerializedName("amount") var amount: Double? = null,
	@SerializedName("receipt") var customerReceipt: Map<String, @RawValue Any?>? = null) : Parcelable

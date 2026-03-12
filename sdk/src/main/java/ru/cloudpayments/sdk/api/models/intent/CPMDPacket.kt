package ru.cloudpayments.sdk.api.models.intent

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CPMDPacket(
	@SerializedName("ThreeDSCallbackId") var threeDSCallbackId: String,
	@SerializedName("TransactionId") var transactionId: Long,
	@SerializedName("SuccessUrl") var successUrl: String = "https://cp.ru/",
	@SerializedName("FailUrl") var failUrl: String = "https://cp.ru/") : Parcelable


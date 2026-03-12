package ru.cloudpayments.sdk.api.models.intent

import com.google.gson.annotations.SerializedName

data class CPTransactionInResponses(
	@SerializedName("transactionId") val transactionId: Long?,
	@SerializedName("paymentMethod") val paymentMethod: String?,
	@SerializedName("puid") val puid: String?,
	@SerializedName("status") val status: String?,
	@SerializedName("code") val code: String?
)

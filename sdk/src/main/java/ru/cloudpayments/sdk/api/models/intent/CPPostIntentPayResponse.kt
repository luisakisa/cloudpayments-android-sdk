package ru.cloudpayments.sdk.api.models.intent

import com.google.gson.annotations.SerializedName

data class CPPostIntentPayResponse(
	@SerializedName("status") val status: String,
	@SerializedName("paReq") val paReq: String,
	@SerializedName("threeDsCallbackId") val threeDsCallbackId: String,
	@SerializedName("acsUrl") val acsUrl: String,
	@SerializedName("transaction") val transaction: CPTransactionInResponses?,
	)


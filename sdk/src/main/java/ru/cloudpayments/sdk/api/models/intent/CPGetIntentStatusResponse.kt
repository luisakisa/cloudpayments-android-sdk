package ru.cloudpayments.sdk.api.models.intent

import com.google.gson.annotations.SerializedName

data class CPGetIntentStatusResponse(
	@SerializedName("status") val status: String,
	@SerializedName("transactions") val transactions: ArrayList<CPTransactionInResponses>?,

	)

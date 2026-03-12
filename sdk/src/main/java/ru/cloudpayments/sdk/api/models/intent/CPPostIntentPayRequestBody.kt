package ru.cloudpayments.sdk.api.models.intent

import com.google.gson.annotations.SerializedName

data class CPPostIntentPayRequestBody(
	@SerializedName("id") val intentId: String,
	@SerializedName("paymentMethod") val paymentMethod: String,
	@SerializedName("cryptogram") val cryptogram: String,
	@SerializedName("saveCard") var saveCard: Boolean? = null) {

	companion object {
		val PAYMENT_METHOD_CARD = "Card"
	}
}
package ru.cloudpayments.sdk.api.models.intent

import com.google.gson.annotations.SerializedName

data class CPPatchIntentItemRequestBody(
	@SerializedName("path") val patch: String,
	@SerializedName("value") val value: Any,
	@SerializedName("op") val op: String = "replace")


package ru.cloudpayments.sdk.api.models.intent

import android.os.Parcelable
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

data class CPPostIntentRequestBody(
	@SerializedName("publicTerminalId") val publicTerminalId: String,
	@SerializedName("amount") val amount: Double,
	@SerializedName("currency") val currency: String,
	@SerializedName("paymentSchema") val paymentSchema: String,
	@SerializedName("description") val description: String?  = null,
	@SerializedName("externalId") val externalId: String?  = null,
	@SerializedName("receiptEmail") val receiptEmail: String?  = null,
	@SerializedName("userInfo") val cpUserInfo: PayerInfo = PayerInfo(),
	@SerializedName("culture") val culture: String = "Ru-ru",
	@SerializedName("type") val type: String = "Default",
	@SerializedName("recurrent") val recurrent: CPRecurrent? = null,
	@SerializedName("receipt") val receipt: Map<String, @RawValue Any?>? = null,
	@SerializedName("paymentMethodSequence") val paymentMethodSequence: ArrayList<String>? = null,
	@SerializedName("metadata") val metadata: JsonObject? = null,
	@SerializedName("scenario") val scenario: Int = 7,
	@SerializedName("paymentUrl") val paymentUrl: String = "https://demo-preprod.cloudpayments.ru",
	@SerializedName("successRedirectUrl") val successRedirectUrl: String = "",
	@SerializedName("failRedirectUrl") val failRedirectUrl: String = "")

@Parcelize
data class PayerInfo(
	@SerializedName("firstName") var firstName: String? = null,
	@SerializedName("lastName") var lastName: String? = null,
	@SerializedName("middleName") var middleName: String? = null,
	@SerializedName("fullName") var fullName: String? = null,
	@SerializedName("birth") var birthDay: String? = null,
	@SerializedName("address") var address: String? = null,
	@SerializedName("street") var street: String? = null,
	@SerializedName("city") var city: String? = null,
	@SerializedName("country") var country: String? = null,
	@SerializedName("phone") var phone: String? = null,
	@SerializedName("postcode") var postcode: String? = null,
	@SerializedName("accountId") var accountId: String? = null,
	@SerializedName("email") var email: String? = null) : Parcelable
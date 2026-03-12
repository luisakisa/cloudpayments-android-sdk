package ru.cloudpayments.sdk.api.models.intent

import com.google.gson.annotations.SerializedName
import ru.cloudpayments.sdk.api.models.SBPBanksItem

data class CPPostIntentResponse(
	@SerializedName("id") val id: String,
	@SerializedName("secret") val secret: String,
	@SerializedName("paymentMethods") val paymentMethods: ArrayList<CPPaymentMethod>?,
	@SerializedName("paymentMethodSequence") val paymentMethodSequence: ArrayList<String>?,
	@SerializedName("terminalInfo") val terminalInfo: CPTerminalInfo?,
	@SerializedName("tokenize") val tokenize: Boolean?
)

data class CPPaymentMethod(
	@SerializedName("type") val type: String?,
	@SerializedName("banks") val banks: java.util.ArrayList<SBPBanksItem>?
	//@SerializedName("GPayGatewayName") val gPayGatewayName: String?
) {
	companion object {
		val CARD = "Card"
		val GOOGLE_PAY = "GooglePay"
		val SBP = "Sbp"
		val T_PAY = "TinkoffPay"
		val MIR_PAY = "MirPay"
		val SBER_PAY = "SberPay"
		val DOLYAME = "Dolyame"
	}
}

data class CPTerminalInfo(
	@SerializedName("terminalFullUrl") val terminalFullUrl: String?,
	@SerializedName("skipExpiryValidation") val skipExpiryValidation: Boolean?,
	@SerializedName("isCvvRequired") val isCvvRequired: Boolean?,
	@SerializedName("isTest") val isTest: Boolean?,
	@SerializedName("features") val features: CPTerminalFeatures?

)

data class CPTerminalFeatures(
	@SerializedName("isAllowedNotSanctionedCards") val isAllowedNotSanctionedCards: Boolean = true,
	@SerializedName("isQiwi") val isQiwi: Boolean = false,
	@SerializedName("isSaveCard") val saveCardMode: String = ""
) {
	companion object {
		val SAVE_CARD_CLASSIC = "Classic" // не отображаем сохранение карты вообще
		val SAVE_CARD_NEW = "New" // не отображаем сохранение карты вообще
		val SAVE_CARD_OPTIONAL = "Optional" //поле отображаем, можно выбрать
		val SAVE_CARD_FORCE = "Force" //поле серое карту сохраняем всегда(приходит по рекуррентам)
	}
}
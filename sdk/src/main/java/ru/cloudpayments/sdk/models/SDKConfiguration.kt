package ru.cloudpayments.sdk.models

import ru.cloudpayments.sdk.api.models.SBPBanksItem

data class SDKConfiguration(
	var publicKey: PublicKey = PublicKey(pem = null, version = null),
	var intentId: String = "",
	var secret: String = "",
	var availablePaymentMethods: ArrayList<String> = ArrayList(),
	var terminalConfiguration: TerminalConfiguration = TerminalConfiguration(),
	var saveCard: Boolean? = null,
	var selectPaymentScreenState: SelectPaymentScreenState = SelectPaymentScreenState()
	)

data class PublicKey(
	var pem: String? = null,
	var version: Int? = null
)

data class TerminalConfiguration(
	var gPayGatewayName: String = "",
	var saveCardMode: String = "",
	var isCvvRequired: Boolean? = null,
	var isAllowedNotSanctionedCards: Boolean? = null,
	var isQiwi: Boolean? = null,
	var skipExpiryValidation: Boolean? = null,
	var banksForSbp: ArrayList<SBPBanksItem>? = null
)

data class SelectPaymentScreenState(
	var collapsePaymentMethodList: Boolean? = null,
	var checkBoxSaveCard: Boolean? = null,
	var checkBoxSendReceipt: Boolean? = null,
	var editTextEmail: String? = null
)
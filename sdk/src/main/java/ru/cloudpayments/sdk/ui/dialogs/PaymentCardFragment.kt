package ru.cloudpayments.sdk.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import ru.cloudpayments.sdk.R
import ru.cloudpayments.sdk.analytics.AnalyticsManager
import ru.cloudpayments.sdk.api.models.intent.CPPaymentMethod
import ru.cloudpayments.sdk.card.Card
import ru.cloudpayments.sdk.card.CardType
import ru.cloudpayments.sdk.configuration.CloudpaymentsSDK
import ru.cloudpayments.sdk.databinding.DialogCpsdkPaymentCardBinding
import ru.cloudpayments.sdk.scanner.CardData
import ru.cloudpayments.sdk.ui.PaymentActivity
import ru.cloudpayments.sdk.ui.dialogs.base.BasePaymentBottomSheetFragment
import ru.cloudpayments.sdk.util.TextWatcherAdapter
import ru.cloudpayments.sdk.util.hideKeyboard
import ru.cloudpayments.sdk.viewmodel.PaymentCardViewModel
import ru.cloudpayments.sdk.viewmodel.PaymentCardViewState
import ru.tinkoff.decoro.MaskDescriptor
import ru.tinkoff.decoro.parser.UnderscoreDigitSlotsParser
import ru.tinkoff.decoro.watchers.DescriptorFormatWatcher

internal class PaymentCardFragment :
	BasePaymentBottomSheetFragment<PaymentCardViewState, PaymentCardViewModel>() {
	interface IPaymentCardFragment {
		fun onPayCardClicked(cryptogram: String)
	}

	companion object {
		const val REQUEST_CODE_SCANNER = 1

		fun newInstance() = PaymentCardFragment().apply {
			arguments = Bundle()
		}
	}

	private var _binding: DialogCpsdkPaymentCardBinding? = null

	private val binding get() = _binding!!

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

		AnalyticsManager.instance.trackEvent(
			name = "events.system",
			parameters = mapOf(
				"eventType" to "Open",
				"screenName" to "/methods/card-edit",
				"cardFieldsCount" to 3,
				"methodChosen" to "Card"
			)
		)

		AnalyticsManager.instance.trackEvent(
			name = "events.system",
			parameters = mapOf(
				"eventType" to "CardInputShown",
				"screenName" to "/methods/card-edit",
				"cardFieldsCount" to 3
			)
		)

		return BottomSheetDialog(requireContext(), R.style.cpsdk_BottomSheetDialogKeyboardTheme)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		_binding = DialogCpsdkPaymentCardBinding.inflate(inflater, container, false)
		val view = binding.root
		return view
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	override val viewModel: PaymentCardViewModel by viewModels()

	override fun render(state: PaymentCardViewState) {
		if (state.isCvvRequired) {
			showCvv()
		} else {
			hideCvv()
		}
	}

	private val cardNumberFormatWatcher by lazy {

		val descriptor = MaskDescriptor.ofRawMask("____ ____ ____ _______")
			.setTerminated(true)
			.setForbidInputWhenFilled(true)

		DescriptorFormatWatcher(UnderscoreDigitSlotsParser(), descriptor)
	}

	private val cardExpFormatWatcher by lazy {
		val descriptor = MaskDescriptor.ofRawMask("__/__")
			.setTerminated(true)
			.setForbidInputWhenFilled(true)

		DescriptorFormatWatcher(UnderscoreDigitSlotsParser(), descriptor)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		activity().component.inject(viewModel)

		cardNumberFormatWatcher.installOn(binding.editCardNumber)
		cardExpFormatWatcher.installOn(binding.editCardExp)

		binding.editCardNumber.addTextChangedListener(object : TextWatcherAdapter() {
			override fun afterTextChanged(s: Editable?) {
				super.afterTextChanged(s)

				val cardNumber = s.toString().replace(" ", "")
				if (Card.isValidNumber(cardNumber)) {
					//edit_card_exp.requestFocus()
					errorModeCardFields(false, binding.editCardNumber, binding.tilCardNumber)
				} else {
					errorModeCardFields(
						cardNumber.length == 19,
						binding.editCardNumber,
						binding.tilCardNumber
					)
				}

				viewModel.getBinInfo(
					cardNumber,
					paymentConfiguration!!.paymentData.amount,
					paymentConfiguration!!.paymentData.currency,
					sdkConfiguration?.terminalConfiguration?.isAllowedNotSanctionedCards ?: true,
					sdkConfiguration?.terminalConfiguration?.isQiwi ?: false
				)

				AnalyticsManager.instance.trackEvent(
					name = "events.action",
					parameters = mapOf(
						"eventType" to "CardTypeDefinition",
						"cardFieldsCount" to 3,
						"context" to "Ru",
						"screenName" to "/methods/card-edit",
						"actionType" to "Fill",
						"methodChosen" to "Card"
					)
				)

				updateForm(cardNumber)
				updateStateButtons()
			}
		})

		binding.editCardNumber.setOnFocusChangeListener { _, hasFocus ->
			errorModeCardFields(
				!hasFocus && !Card.isValidNumber(binding.editCardNumber.text.toString()),
				binding.editCardNumber, binding.tilCardNumber
			)

			if (hasFocus && binding.editCardNumber.text.toString().isEmpty()) {
				AnalyticsManager.instance.trackEvent(
					name = "events.action",
					parameters = mapOf(
						"elementLabel" to "CardDataFillStarted",
						"elementType" to "Input",
						"screenName" to "/methods/card-edit",
						"actionType" to "Fill",
						"methodChosen" to "Card"
					)
				)
			}

			if (!hasFocus && Card.isValidNumber(binding.editCardNumber.text.toString())) {
				AnalyticsManager.instance.trackEvent(
					name = "events.action",
					parameters = mapOf(
						"eventType" to "Input",
						"screenName" to "/methods/card-edit",
					  	"actionType" to "Fill",
						"methodChosen" to "Card"
					)
				)
			}
		}

		binding.editCardExp.addTextChangedListener(object : TextWatcherAdapter() {
			override fun afterTextChanged(s: Editable?) {
				super.afterTextChanged(s)

				val cardExp = s.toString()
				if (Card.isValidExpDate(
						cardExp,
						sdkConfiguration?.terminalConfiguration?.skipExpiryValidation
					)
				) {
					//edit_card_cvv.requestFocus()
					errorModeCardFields(false, binding.editCardExp, binding.tilCardExp)
				} else {
					errorModeCardFields(cardExp.length == 5, binding.editCardExp, binding.tilCardExp)
				}
				updateStateButtons()
			}
		})

		binding.editCardExp.setOnFocusChangeListener { _, hasFocus ->
			errorModeCardFields(
				!hasFocus && !Card.isValidExpDate(
					binding.editCardExp.text.toString(),
					sdkConfiguration?.terminalConfiguration?.skipExpiryValidation
				),
				binding.editCardExp, binding.tilCardExp
			)

			if (!hasFocus && Card.isValidExpDate(binding.editCardExp.text.toString(),
												 sdkConfiguration?.terminalConfiguration?.skipExpiryValidation)) {
				AnalyticsManager.instance.trackEvent(
					name = "events.action",
					parameters = mapOf(
						"elementLabel" to "ExpiredDate",
						"elementType" to "Input",
						"screenName" to "/methods/card-edit",
						"actionType" to "Fill",
						"methodChosen" to "Card"
					)
				)
			}
		}

		binding.editCardCvv.addTextChangedListener(object : TextWatcherAdapter() {
			override fun afterTextChanged(s: Editable?) {
				super.afterTextChanged(s)
				errorModeCardFields(false, binding.editCardCvv, binding.tilCardCvv)

				if (Card.isValidCvv(s.toString(), binding.tilCardCvv.isVisible)) {
					requireActivity().hideKeyboard()
				}
				updateStateButtons()
			}
		})

		binding.editCardCvv.setOnFocusChangeListener { _, hasFocus ->
			errorModeCardFields(
				!hasFocus && !Card.isValidCvv(
					binding.editCardCvv.text.toString(),
					binding.tilCardCvv.isVisible
				), binding.editCardCvv, binding.tilCardCvv
			)

			if (!hasFocus && Card.isValidCvv(binding.editCardCvv.text.toString(), binding.tilCardCvv.isVisible)) {
				AnalyticsManager.instance.trackEvent(
					name = "events.action",
					parameters = mapOf(
						"elementLabel" to "CVV",
						"elementType" to "Input",
						"screenName" to "/methods/card-edit",
						"actionType" to "Fill",
						"methodChosen" to "Card"
					)
				)
			}
		}

		binding.buttonPay.setOnClickListener {
			val cardNumber = binding.editCardNumber.text.toString()
			val cardExp = binding.editCardExp.text.toString()
			val cardCvv = binding.editCardCvv.text.toString()

			val cryptogram = Card.createHexPacketFromData(
				cardNumber,
				cardExp,
				cardCvv,
				paymentConfiguration?.publicId ?: "",
				sdkConfiguration?.publicKey?.pem ?: "",
				sdkConfiguration?.publicKey?.version ?: 0
			)

			if (isValid() && cryptogram != null) {

				val listener = requireActivity() as? IPaymentCardFragment
				listener?.onPayCardClicked(cryptogram)
				dismiss()
			}
		}

		binding.btnScan.setOnClickListener {
			val intent = paymentConfiguration?.scanner?.getScannerIntent(requireContext())
			if (intent != null) {
				startActivityForResult(intent, REQUEST_CODE_SCANNER)
			}
		}

//		binding.buttonPay.text = getString(
//			R.string.cpsdk_text_card_pay_button,
//			String.format(
//				"%.2f " + Currency.getSymbol(paymentConfiguration!!.paymentData.currency),
//				paymentConfiguration!!.paymentData.amount.toDouble()
//			)
//		)

		updateForm("")
		updateStateButtons()
	}

	private fun updatePaymentSystemIcon(cardNumber: String) {
		val cardType = CardType.getType(cardNumber)
		val psIcon = cardType.getIconRes()
		if (paymentConfiguration?.scanner != null && (cardNumber.isEmpty() || psIcon == null)) {
			binding.icPs.isVisible = false
			binding.btnScan.isVisible = false
		} else {
			binding.icPs.isVisible = true
			binding.btnScan.isVisible = false
			binding.icPs.setImageResource(psIcon ?: 0)
		}
	}

	private fun updateStateButtons() {
		if (isValid()) {
			enableButtons()

			AnalyticsManager.instance.trackEvent(
				name = "events.action",
				parameters = mapOf(
					"context" to "Ru",
					"elementLabel" to "CardDataFillFinished",
					"elementType" to "Input",
					"screenName" to "/methods/card-edit",
					"actionType" to "Fill",
					"methodChosen" to "Card"
				)
			)

		} else {
			disableButtons()
		}
	}

	private fun disableButtons() {
		binding.viewBlockButtons.visibility = View.VISIBLE
	}

	private fun enableButtons() {
		binding.viewBlockButtons.visibility = View.GONE
	}

	private fun isValid(): Boolean {
		val cardNumber = binding.editCardNumber.text.toString()
		val cardNumberIsValid = Card.isValidNumber(cardNumber)
		val cardExpIsValid = Card.isValidExpDate(
			binding.editCardExp.text.toString(),
			sdkConfiguration?.terminalConfiguration?.skipExpiryValidation
		)
		val cardCvvIsValid =
			Card.isValidCvv(binding.editCardCvv.text.toString(), binding.tilCardCvv.isVisible)

//		errorModeCardFields(!cardNumberIsValid, binding.editCardNumber)
//		errorModeCardFields(!cardExpIsValid, binding.editCardExp)
//		errorModeCardFields(!cardCvvIsValid, binding.editCardCvv)

		return cardNumberIsValid && cardExpIsValid && cardCvvIsValid
	}

	private fun updateWithCardData(cardData: CardData) {
		binding.editCardNumber.setText(cardData.cardNumber)
		binding.editCardExp.setText("${cardData.cardExpMonth}/${cardData.cardExpYear}")
	}

	private fun updateForm(cardNumber: String) {

		updatePaymentSystemIcon(cardNumber)

		if (cardNumber.length < 4) {
			if (sdkConfiguration?.terminalConfiguration?.isCvvRequired == true) {
				showCvv()
			} else {
				hideCvv()
			}
		}
	}

	private fun hideCvv() {
		binding.editCardCvv.setText("")
		binding.tilCardCvv.visibility = View.GONE
	}

	private fun showCvv() {
		binding.tilCardCvv.visibility = View.VISIBLE
	}

	@Deprecated("Deprecated in Java")
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
		when (requestCode) {
			REQUEST_CODE_SCANNER -> {
				if (data != null) {
					val cardData = paymentConfiguration?.scanner?.getCardDataFromIntent(data)
					if (cardData != null) {
						updateWithCardData(cardData)
					}
				}

				super.onActivityResult(requestCode, resultCode, data)
			}

			else -> super.onActivityResult(requestCode, resultCode, data)
		}

	override fun onCancel(dialog: DialogInterface) {
		super.onCancel(dialog)

		AnalyticsManager.instance.trackEvent(
			name = "events.system",
			parameters = mapOf(
				"eventType" to "Close",
				"screenName" to "/methods/card-edit",
				"methodChosen" to "Сard",
				"cardFieldsCount" to 3
			)
		)

		if (paymentConfiguration?.singlePaymentMode == CPPaymentMethod.CARD) {
			activity?.finish()
		} else {
			(activity as PaymentActivity).showPaymentOptions()
		}
	}
}
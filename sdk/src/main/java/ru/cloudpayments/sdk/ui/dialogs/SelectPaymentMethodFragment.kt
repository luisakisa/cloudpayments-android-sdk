package ru.cloudpayments.sdk.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import ru.cloudpayments.sdk.R
import ru.cloudpayments.sdk.analytics.AnalyticsManager
import ru.cloudpayments.sdk.api.models.intent.CPPaymentMethod
import ru.cloudpayments.sdk.api.models.intent.CPTerminalFeatures
import ru.cloudpayments.sdk.configuration.EmailBehavior
import ru.cloudpayments.sdk.databinding.DialogCpsdkSelectPaymentMethodBinding
import ru.cloudpayments.sdk.models.ApiError
import ru.cloudpayments.sdk.ui.PaymentActivity
import ru.cloudpayments.sdk.ui.dialogs.base.BasePaymentBottomSheetFragment
import ru.cloudpayments.sdk.util.InjectorUtils
import ru.cloudpayments.sdk.util.TextWatcherAdapter
import ru.cloudpayments.sdk.util.hideKeyboard
import ru.cloudpayments.sdk.util.isEmailValid
import ru.cloudpayments.sdk.viewmodel.SelectPaymentMethodViewModel
import ru.cloudpayments.sdk.viewmodel.SelectPaymentMethodViewState

internal enum class SelectPaymentMethodStatus {
	Waiting,
	CardLoading,
	CardSuccess,
	TPayLoading,
	TPaySuccess,
	SberPayLoading,
	SberPaySuccess,
	SbpLoading,
	SbpSuccess,
	MirPayLoading,
	MirPaySuccess,
	DolyameLoading,
	DolyameSuccess,
	AlreadyPaid,
	Failed;
}

internal class SelectPaymentMethodFragment :
	BasePaymentBottomSheetFragment<SelectPaymentMethodViewState, SelectPaymentMethodViewModel>() {
	interface ISelectPaymentMethodFragment {
		fun runCardPayment()
		fun runTPay(payUrl: String, transactionUuid: String)
		fun runSberPay(payUrl: String, transactionUuid: String)
		fun runSbp()
		fun runMirPay(deepLink: String, transactionUuid: String)
		fun runDolyame(payUrl: String, transactionUuid: String)
		fun runGooglePay()
		fun onAltAlreadyPaid(transactionId: Long?)
		fun onAltPayLinkError(transactionId: Long?, errorCode: String?)
	}

	companion object {
		fun newInstance() = SelectPaymentMethodFragment().apply {
			arguments = Bundle()
		}
	}

	private var _binding: DialogCpsdkSelectPaymentMethodBinding? = null

	private val binding get() = _binding!!
	private var statistEmailChanged = false

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

		val methodsDisplayed = sdkConfiguration?.availablePaymentMethods?.let {

			if (it.size <= 3) {
				it
			} else {
				it.take(2)
			}
		}

		AnalyticsManager.instance.trackEvent(
			name = "events.system",
			parameters = mapOf(
				"eventType" to "Open",
				"screenName" to "/methods",
				"cardFieldsCount" to 0,
				"methodsAvailable" to sdkConfiguration?.availablePaymentMethods,
				"methodsDisplayed" to methodsDisplayed
			)
		)

		return super.onCreateDialog(savedInstanceState)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		_binding = DialogCpsdkSelectPaymentMethodBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	override val viewModel: SelectPaymentMethodViewModel by viewModels {
		InjectorUtils.provideSelectPaymentMethodViewModelFactory(paymentConfiguration!!)
	}

	override fun render(state: SelectPaymentMethodViewState) {

		updateWith(state)
	}

	private fun updateWith(state: SelectPaymentMethodViewState) {

		val state = state

		binding.layoutButtons.apply {
			setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
			setContent {
				ButtonsView(state)
			}
		}

		when (state.status) {
			SelectPaymentMethodStatus.Waiting -> {

			}
			SelectPaymentMethodStatus.CardLoading -> {
				disableAllButtons()
			}
			SelectPaymentMethodStatus.CardSuccess -> {
				enableAllButtons()
				val listener = requireActivity() as? ISelectPaymentMethodFragment
				listener?.runCardPayment()
				dismiss()
			}
			SelectPaymentMethodStatus.TPayLoading -> {
				disableAllButtons()
			}
			SelectPaymentMethodStatus.TPaySuccess -> {
				enableAllButtons()
				val listener = requireActivity() as? ISelectPaymentMethodFragment
				val payUrl = viewModel.currentState.payUrl
				val transactionUuid = viewModel.currentState.transactionUuid
				if (payUrl != null && transactionUuid != null) {
					listener?.runTPay(payUrl, transactionUuid)
					dismiss()
				}
			}
			SelectPaymentMethodStatus.SberPayLoading -> {
				disableAllButtons()
			}
			SelectPaymentMethodStatus.SberPaySuccess -> {
				enableAllButtons()
				val listener = requireActivity() as? ISelectPaymentMethodFragment
				val payUrl = viewModel.currentState.payUrl
				val transactionUuid = viewModel.currentState.transactionUuid
				if (payUrl != null && transactionUuid != null) {
					listener?.runSberPay(payUrl, transactionUuid)
					dismiss()
				}
			}
			SelectPaymentMethodStatus.SbpLoading -> {
				disableAllButtons()
			}
			SelectPaymentMethodStatus.SbpSuccess -> {
				enableAllButtons()
				val listener = requireActivity() as? ISelectPaymentMethodFragment
				listener?.runSbp()
				dismiss()
			}
			SelectPaymentMethodStatus.MirPayLoading -> {
				disableAllButtons()
			}
			SelectPaymentMethodStatus.MirPaySuccess -> {
				enableAllButtons()

				val listener = requireActivity() as? ISelectPaymentMethodFragment
				val deepLink =  viewModel.currentState.payUrl
				val transactionUuid = viewModel.currentState.transactionUuid
				if (deepLink != null && transactionUuid != null) {
					listener?.runMirPay(deepLink, transactionUuid)
					dismiss()
				}
			}
			SelectPaymentMethodStatus.DolyameLoading -> {
				disableAllButtons()
			}
			SelectPaymentMethodStatus.DolyameSuccess -> {
				enableAllButtons()
				val listener = requireActivity() as? ISelectPaymentMethodFragment
				val payUrl = viewModel.currentState.payUrl
				val transactionUuid = viewModel.currentState.transactionUuid
				if (payUrl != null && transactionUuid != null) {
					listener?.runDolyame(payUrl, transactionUuid)
					dismiss()
				}
			}
			SelectPaymentMethodStatus.AlreadyPaid -> {
				enableAllButtons()

				dismiss()

				val listener = requireActivity() as? ISelectPaymentMethodFragment
				listener?.onAltAlreadyPaid(viewModel.currentState.transactionId)
			}
			SelectPaymentMethodStatus.Failed -> {
				enableAllButtons()

				dismiss()

				if (state.reasonCode == ApiError.CODE_ERROR_CONNECTION) {
					val listener = requireActivity() as? PaymentActivity
					listener?.onInternetConnectionError()
				} else {
					val listener = requireActivity() as? ISelectPaymentMethodFragment
					listener?.onAltPayLinkError(viewModel.currentState.transactionId, state.reasonCode)
				}
			}
		}
	}

	private fun setSaveCardMode () {

		when (sdkConfiguration?.terminalConfiguration?.saveCardMode) {
			CPTerminalFeatures.SAVE_CARD_CLASSIC -> {

			}
			CPTerminalFeatures.SAVE_CARD_NEW -> {

			}
			CPTerminalFeatures.SAVE_CARD_OPTIONAL -> {
				setSaveCardCheckBoxVisible()
			}
			CPTerminalFeatures.SAVE_CARD_FORCE -> {
				setSaveCardHintVisible()
			}
			else -> {

			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		activity().component.inject(viewModel)

		setSaveCardMode()

		binding.editEmail.setText(paymentConfiguration!!.paymentData.email)
		errorMode(
			!binding.editEmail.hasFocus() && !isEmailValid(binding.editEmail.text.toString()),
			binding.editEmail, binding.textFieldEmail
		)

		binding.editEmail.setOnFocusChangeListener { _, hasFocus ->
			errorMode(
				!hasFocus && !isEmailValid(binding.editEmail.text.toString()),
				binding.editEmail, binding.textFieldEmail
			)

			showEmailErrorMessage()
		}

		binding.editEmail.addTextChangedListener(object : TextWatcherAdapter() {
			override fun afterTextChanged(s: Editable?) {
				super.afterTextChanged(s)
				updateStateButtons()

				if (isEmailValid(binding.editEmail.text.toString())) {

					statistEmailChanged = true
				}
			}
		})

		when (paymentConfiguration!!.emailBehavior) {
			EmailBehavior.OPTIONAL -> {
				binding.textSendReceipt.visibility = View.VISIBLE
				binding.materialSwitchSendReceipt.visibility = View.VISIBLE
				if (paymentConfiguration!!.paymentData.email.isNullOrEmpty()) {
					binding.materialSwitchSendReceipt.isChecked = false
					binding.textFieldEmail.visibility = View.GONE
				} else {
					binding.materialSwitchSendReceipt.isChecked = true
					binding.textFieldEmail.visibility = View.VISIBLE
				}
			}
			EmailBehavior.REQUIRED -> {
				binding.textSendReceipt.visibility = View.GONE
				binding.materialSwitchSendReceipt.visibility = View.GONE
				binding.textFieldEmail.visibility = View.VISIBLE
			}
			EmailBehavior.HIDDEN -> {
				binding.textSendReceipt.visibility = View.GONE
				binding.materialSwitchSendReceipt.visibility = View.GONE
				binding.textFieldEmail.visibility = View.GONE
			}
		}

		binding.materialSwitchSendReceipt.setOnCheckedChangeListener { _, isChecked ->
			binding.textFieldEmail.isGone = !isChecked
			showEmailErrorMessage()
			requireActivity().hideKeyboard()
			updateStateButtons()

			AnalyticsManager.instance.trackEvent(
				name = "events.action",
				parameters = mapOf(
					"context" to if (isChecked)  "On" else "Off",
					"elementLabel" to "SendEmail",
					"elementType" to "Checkbox",
					"screenName" to "/methods",
					"actionType" to "Click"
				)
			)
		}

		updateStateButtons()

		binding.buttonSaveCardPopup.setOnClickListener {
			showPopupSaveCardInfo()
		}

		restoreScreenState()
	}

	private fun showEmailErrorMessage() {

		if (binding.textFieldEmail.isGone) {
			binding.textEmailError.visibility = View.GONE
		} else {

			if (!binding.editEmail.hasFocus() && !isEmailValid(binding.editEmail.text.toString())) {
				binding.textEmailError.visibility = View.VISIBLE
				if (binding.editEmail.text.toString().isEmpty()) {
					binding.textEmailError.text = getString(R.string.cpsdk_text_options_email_empty)
				} else {
					binding.textEmailError.text = getString(R.string.cpsdk_text_options_email_error)
				}
			} else {
				binding.textEmailError.visibility = View.GONE
			}
		}
	}

	private fun restoreScreenState() {
		if (sdkConfiguration?.selectPaymentScreenState?.collapsePaymentMethodList != null && sdkConfiguration?.selectPaymentScreenState?.collapsePaymentMethodList == false) {
			viewModel.expandPaymentMethods()
		}
		if (sdkConfiguration?.selectPaymentScreenState?.checkBoxSaveCard != null) {
			binding.materialSwitchSaveCard.isChecked = sdkConfiguration?.selectPaymentScreenState?.checkBoxSaveCard!!
		}
		if (sdkConfiguration?.selectPaymentScreenState?.checkBoxSendReceipt != null) {
			binding.materialSwitchSendReceipt.isChecked = sdkConfiguration?.selectPaymentScreenState?.checkBoxSendReceipt!!
		}
		if (sdkConfiguration?.selectPaymentScreenState?.editTextEmail != null) {
			binding.editEmail.setText(sdkConfiguration?.selectPaymentScreenState?.editTextEmail)
		}
	}

	private fun updateEmail() {
		if (binding.editEmail.isVisible) {
			paymentConfiguration?.paymentData?.email = binding.editEmail.text.toString()
		} else {
			paymentConfiguration?.paymentData?.email = ""
		}
	}

	private fun updateSaveCard() {
		if (binding.materialSwitchSaveCard.isVisible) {
			sdkConfiguration?.saveCard = binding.materialSwitchSaveCard.isChecked
		}
	}

	private fun saveScreenState() {
		sdkConfiguration?.selectPaymentScreenState?.collapsePaymentMethodList = viewModel.currentState.collapsePaymentMethods

		if (binding.materialSwitchSaveCard.isVisible) {
			sdkConfiguration?.selectPaymentScreenState?.checkBoxSaveCard = binding.materialSwitchSaveCard.isChecked
		}

		if (binding.materialSwitchSendReceipt.isVisible) {
			sdkConfiguration?.selectPaymentScreenState?.checkBoxSendReceipt = binding.materialSwitchSendReceipt.isChecked
		}

		sdkConfiguration?.selectPaymentScreenState?.editTextEmail = binding.editEmail.text.toString()
	}

	private fun runPay(paymentMethod: String) {

		if (statistEmailChanged) {
			AnalyticsManager.instance.trackEvent(
				name = "events.action",
				parameters = mapOf(
					"context" to binding.editEmail.text.toString(),
					"elementLabel" to "Email",
					"elementType" to "Input",
					"screenName" to "/methods",
					"actionType" to "Fill"
				)
			)
		}

		saveScreenState()
		updateEmail()
		updateSaveCard()

		val email = if (binding.editEmail.isVisible) {
			paymentConfiguration?.paymentData?.email
		} else {
			null
		}

		val saveCard = if (binding.materialSwitchSaveCard.isVisible) {
			sdkConfiguration?.saveCard
		} else {
			null
		}

		viewModel.patchIntentAndRunPay(paymentMethod,  sdkConfiguration?.secret.toString(), sdkConfiguration?.intentId.toString(), email, saveCard)
	}

	private fun expandPaymentMethods() {
		viewModel.expandPaymentMethods()
		updateHeight()
	}

	private fun collapsePaymentMethods() {
		viewModel.collapsePaymentMethods()
	}

	private fun setSaveCardCheckBoxVisible() {
		binding.textSaveCard.visibility = View.VISIBLE
		binding.textSaveCard.text = getString(R.string.cpsdk_text_options_save_card)
		binding.buttonSaveCardPopup.visibility = View.VISIBLE
		binding.materialSwitchSaveCard.visibility = View.VISIBLE
		binding.materialSwitchSaveCard.isChecked = false
	}

	private fun setSaveCardHintVisible() {
		binding.textSaveCard.visibility = View.VISIBLE
		binding.textSaveCard.text = getString(R.string.cpsdk_text_options_card_be_saved)
		binding.buttonSaveCardPopup.visibility = View.VISIBLE
		binding.materialSwitchSaveCard.visibility = View.INVISIBLE
	}

	private fun showPopupSaveCardInfo() {
		val popupView = layoutInflater.inflate(R.layout.popup_cpsdk_save_card_info, null)

		val wid = LinearLayout.LayoutParams.WRAP_CONTENT
		val high = LinearLayout.LayoutParams.WRAP_CONTENT
		val focus= true
		val popupWindow = PopupWindow(popupView, wid, high, focus)

		val background = activity?.let { ContextCompat.getDrawable(it, R.drawable.cpsdk_bg_popup) }
		popupView.background = background

		popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
	}

	private fun updateStateButtons() {
		if (isFormValid()) {
			enableAllButtons()
		} else {
			disableAllButtons()
		}
	}

	private fun isFormValid(): Boolean {
		val valid = if (binding.textFieldEmail.isVisible) {
			isEmailValid(binding.editEmail.text.toString())
		} else {
			true
		}

		return valid
	}

	private fun disableAllButtons() {
		binding.viewBlockButtons.visibility = View.VISIBLE
	}

	private fun enableAllButtons() {
		binding.viewBlockButtons.visibility = View.GONE
	}

	override fun onCancel(dialog: DialogInterface) {
		super.onCancel(dialog)
		activity?.finish()
	}

	@Composable
	private fun ButtonsView(state: SelectPaymentMethodViewState) {

		var columnHeightPx = 0

		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
			modifier = Modifier
				.onGloballyPositioned { coordinates ->
					// Set column height using the LayoutCoordinates
					columnHeightPx = coordinates.size.height
				}
				.fillMaxWidth()
				.wrapContentHeight()
				.clip(
					RoundedCornerShape(
						topStart = 0.dp,
						topEnd = 0.dp,
						bottomStart = 0.dp,
						bottomEnd = 0.dp
					)
				)
				.background(Color.Transparent)

				.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)

				.alpha(1f)
		) {

			sdkConfiguration?.availablePaymentMethods?.let {

				if (it.size <= 3) {
					DrawPaymentButtons(it, state)
				} else if (state.collapsePaymentMethods) {
					DrawPaymentButtons(it.take(2), state)
					OtherMethodsButtonView(it.drop(2))
				} else {
					DrawPaymentButtons(it, state)
					CollapseButtonView()
				}
			}
		}

		val params = binding.layoutButtons.layoutParams
		params.height = columnHeightPx

		binding.layoutButtons.setLayoutParams(params)
	}

	@Composable
	private fun DrawPaymentButtons(availablePaymentMethods: List<String>, state: SelectPaymentMethodViewState) {
		for (type in availablePaymentMethods) {
			when (type) {
				CPPaymentMethod.CARD -> CardPayButtonView(state.status == SelectPaymentMethodStatus.CardLoading)
				CPPaymentMethod.T_PAY -> TPayButtonView(state.status == SelectPaymentMethodStatus.TPayLoading)
				CPPaymentMethod.SBER_PAY -> SberPayButtonView(state.status == SelectPaymentMethodStatus.SberPayLoading)
				CPPaymentMethod.MIR_PAY -> MirPayButtonView(state.status == SelectPaymentMethodStatus.MirPayLoading)
				CPPaymentMethod.SBP -> SBPButtonView(state.status == SelectPaymentMethodStatus.SbpLoading)
				CPPaymentMethod.DOLYAME -> DolyameButtonView(state.status == SelectPaymentMethodStatus.DolyameLoading)
			}
		}
	}

	@Composable
	private fun CardPayButtonView(isLoading: Boolean) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
			modifier = Modifier
				.fillMaxWidth()
				.height(56.dp)
				.clip(
					RoundedCornerShape(
						topStart = 8.dp,
						topEnd = 8.dp,
						bottomStart = 8.dp,
						bottomEnd = 8.dp
					)
				)
				.background(
					Color(
						red = 0.18039216101169586f,
						green = 0.4431372582912445f,
						blue = 0.9882352948188782f,
						alpha = 1f
					)
				)
				.padding(start = 0.dp, top = 18.dp, end = 0.dp, bottom = 18.dp)
				.alpha(1f)
				.clickable {
					runPay(CPPaymentMethod.CARD)
				}
		) {
			if (isLoading) {
				CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
			} else {
				Text(
					text = stringResource(R.string.cpsdk_text_select_payment_method_card),
					textAlign = TextAlign.Center,
					fontSize = 17.sp,
					textDecoration = TextDecoration.None,
					letterSpacing = -0.4300000071525574.sp,
					lineHeight = 22.sp,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier
						.width(153.dp)
						//.height(22.dp)
						.alpha(1f),
					color = Color(red = 1f, green = 1f, blue = 1f, alpha = 1f),
					fontWeight = FontWeight.Normal,
					fontStyle = FontStyle.Normal,
				)
			}
		}
	}

	@Composable
	private fun TPayButtonView(isLoading: Boolean) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
			modifier = Modifier
				.fillMaxWidth()
				.height(56.dp)
				.clip(
					RoundedCornerShape(
						topStart = 8.dp,
						topEnd = 8.dp,
						bottomStart = 8.dp,
						bottomEnd = 8.dp
					)
				)
				.background(
					Color(
						red = 1f,
						green = 0.8666666746139526f,
						blue = 0.1764705926179886f,
						alpha = 1f
					)
				)
				.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 0.dp)
				.alpha(1f)
				.clickable {
					runPay(CPPaymentMethod.T_PAY)
				}
		) {
			if (isLoading) {
				CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
			} else {
				Box(
					modifier = Modifier
						.width(71.dp)
						.height(30.dp)
						.clip(
							RoundedCornerShape(
								topStart = 0.dp,
								topEnd = 0.dp,
								bottomStart = 0.dp,
								bottomEnd = 0.dp
							)
						)
						.background(Color.Transparent)
						.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
						.alpha(1f),
					contentAlignment = Alignment.Center
				) {
					Image(
						painter = painterResource(id = R.drawable.cpsdk_select_t_pay_button),
						contentDescription = "Success"
					)
				}
			}
		}
	}

	@Composable
	private fun SberPayButtonView(isLoading: Boolean) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
			modifier = Modifier
				.fillMaxWidth()
				.height(56.dp)
				.clip(
					RoundedCornerShape(
						topStart = 8.dp,
						topEnd = 8.dp,
						bottomStart = 8.dp,
						bottomEnd = 8.dp
					)
				)
				.background(
					Color(0xff21a038)
				)
				.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 0.dp)
				.alpha(1f)
				.clickable {
					runPay(CPPaymentMethod.SBER_PAY)
				}
		) {
			if (isLoading) {
				CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
			} else {
				Box(
					modifier = Modifier
						.width(71.dp)
						.height(30.dp)
						.clip(
							RoundedCornerShape(
								topStart = 0.dp,
								topEnd = 0.dp,
								bottomStart = 0.dp,
								bottomEnd = 0.dp
							)
						)
						.background(Color.Transparent)
						.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
						.alpha(1f),
					contentAlignment = Alignment.Center
				) {
					Image(
						painter = painterResource(id = R.drawable.cpsdk_select_sber_pay_button),
						contentDescription = "Success"
					)
				}
			}
		}
	}

	@Composable
	private fun MirPayButtonView(isLoading: Boolean) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
			modifier = Modifier
				.fillMaxWidth()
				.height(56.dp)
				.clip(
					RoundedCornerShape(
						topStart = 8.dp,
						topEnd = 8.dp,
						bottomStart = 8.dp,
						bottomEnd = 8.dp
					)
				)
				.background(
					Color(0xff006848)
				)
				.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 0.dp)
				.alpha(1f)
				.clickable {
					runPay(CPPaymentMethod.MIR_PAY)
				}
		) {
			if (isLoading) {
				CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
			} else {
				Box(
					modifier = Modifier
						.width(100.dp)
						.height(30.dp)
						.clip(
							RoundedCornerShape(
								topStart = 0.dp,
								topEnd = 0.dp,
								bottomStart = 0.dp,
								bottomEnd = 0.dp
							)
						)
						.background(Color.Transparent)
						.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
						.alpha(1f),
					contentAlignment = Alignment.Center
				) {
					Image(
						painter = painterResource(id = R.drawable.cpsdk_select_mir_pay_button),
						contentDescription = "Success"
					)
				}
			}
		}
	}

	@Composable
	private fun SBPButtonView(isLoading: Boolean) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
			modifier = Modifier
				.fillMaxWidth()
				.height(56.dp)
				.clip(
					RoundedCornerShape(
						topStart = 8.dp,
						topEnd = 8.dp,
						bottomStart = 8.dp,
						bottomEnd = 8.dp
					)
				)
				.background(
					Color(0xff1d1346)
				)
				.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 0.dp)
				.alpha(1f)
				.clickable {
					runPay(CPPaymentMethod.SBP)
				}
		) {
			if (isLoading) {
				CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
			} else {
				Box(
					modifier = Modifier
						.width(71.dp)
						.height(30.dp)
						.clip(
							RoundedCornerShape(
								topStart = 0.dp,
								topEnd = 0.dp,
								bottomStart = 0.dp,
								bottomEnd = 0.dp
							)
						)
						.background(Color.Transparent)
						.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
						.alpha(1f),
					contentAlignment = Alignment.Center
				) {
					Image(
						painter = painterResource(id = R.drawable.cpsdk_select_sbp_button),
						contentDescription = "Success"
					)
				}
			}
		}
	}

	@Composable
	private fun DolyameButtonView(isLoading: Boolean) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
			modifier = Modifier
				.fillMaxWidth()
				.height(56.dp)
				.clip(
					RoundedCornerShape(
						topStart = 8.dp,
						topEnd = 8.dp,
						bottomStart = 8.dp,
						bottomEnd = 8.dp
					)
				)
				.background(
					Color(0xff1d1d1d)
				)
				.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 0.dp)
				.alpha(1f)
				.clickable {
					runPay(CPPaymentMethod.DOLYAME)
				}
		) {
			if (isLoading) {
				CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
			} else {
				Box(
					modifier = Modifier
						.width(171.dp)
						.height(44.dp)
						.clip(
							RoundedCornerShape(
								topStart = 0.dp,
								topEnd = 0.dp,
								bottomStart = 0.dp,
								bottomEnd = 0.dp
							)
						)
						.background(Color.Transparent)
						.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
						.alpha(1f),
					contentAlignment = Alignment.Center
				) {
					Image(
						painter = painterResource(id = R.drawable.cpsdk_select_dolyame_button),
						contentDescription = "Success"
					)
				}
			}
		}
	}

	@Composable
	fun OtherMethodsButtonView(paymentMethods: List<String>) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween,
			modifier = Modifier
				.fillMaxWidth()
				.height(56.dp)
				.clip(
					RoundedCornerShape(
						topStart = 8.dp,
						topEnd = 8.dp,
						bottomStart = 8.dp,
						bottomEnd = 8.dp
					)
				)
				.background(Color.Transparent)
				.border(
					1.dp,
					Color(
						red = 0.18039216101169586f,
						green = 0.4431372582912445f,
						blue = 0.9882352948188782f,
						alpha = 1f
					),
					RoundedCornerShape(
						topStart = 8.dp,
						topEnd = 8.dp,
						bottomStart = 8.dp,
						bottomEnd = 8.dp
					)
				)
				.padding(start = 16.dp, top = 13.dp, end = 12.dp, bottom = 13.dp)
				.alpha(1f)
				.clickable {
					expandPaymentMethods()
				}
		) {
			Text(
				text = stringResource(R.string.cpsdk_text_select_payment_method_other_methods),
				textAlign = TextAlign.Center,
				fontSize = 17.sp,
				textDecoration = TextDecoration.None,
				letterSpacing = -0.4300000071525574.sp,
				lineHeight = 22.sp,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier
					.width(133.dp)
					.alpha(1f),
				color = Color(red = 0.18039216101169586f, green = 0.4431372582912445f, blue = 0.9882352948188782f, alpha = 1f),
				fontWeight = FontWeight.Normal,
				fontStyle = FontStyle.Normal,
			)

			CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy((-14.dp), Alignment.Start),

					modifier = Modifier
						.fillMaxWidth()
						.height(56.dp)
						.clip(
							RoundedCornerShape(
								topStart = 8.dp,
								topEnd = 8.dp,
								bottomStart = 8.dp,
								bottomEnd = 8.dp
							)
						)
						.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
						.background(Color.Transparent)
						.alpha(1f)

				) {

					for (method in paymentMethods.reversed()) {
						when (method) {
							CPPaymentMethod.CARD -> OtherMethodIcon(method)
							CPPaymentMethod.T_PAY -> OtherMethodIcon(method)
							CPPaymentMethod.SBER_PAY -> OtherMethodIcon(method)
							CPPaymentMethod.MIR_PAY -> OtherMethodIcon(method)
							CPPaymentMethod.SBP -> OtherMethodIcon(method)
							CPPaymentMethod.DOLYAME -> OtherMethodIcon(method)
						}
					}
				}
			}
		}
	}

	@Composable
	fun OtherMethodIcon(method: String) {

		Box(
			modifier = Modifier
				.width(36.dp)
				.height(36.dp)
				.background(Color.Transparent)
				.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
				.alpha(1f),
			contentAlignment = Alignment.Center
		) {

			when (method) {
				CPPaymentMethod.CARD ->
					Image(
						painter = painterResource(id = R.drawable.cpsdk_other_card_icon),
						contentDescription = "Type"
					)
				CPPaymentMethod.T_PAY ->
					Image(
						painter = painterResource(id = R.drawable.cpsdk_other_t_pay_icon),
						contentDescription = "Type"
					)
				CPPaymentMethod.SBER_PAY ->
					Image(
						painter = painterResource(id = R.drawable.cpsdk_other_sber_pay_icon),
						contentDescription = "Type"
					)
				CPPaymentMethod.MIR_PAY ->
					Image(
						painter = painterResource(id = R.drawable.cpsdk_other_mir_pay_icon),
						contentDescription = "Type"
					)
				CPPaymentMethod.SBP ->
					Image(
						painter = painterResource(id = R.drawable.cpsdk_other_sbp_icon),
						contentDescription = "Type"
					)
				CPPaymentMethod.DOLYAME ->
					Image(
						painter = painterResource(id = R.drawable.cpsdk_other_dolyame_icon),
						contentDescription = "Type"
					)
			}
		}
	}

	@Composable
	fun CollapseButtonView() {
		Row(
			verticalAlignment = Alignment.Top,
			horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
			modifier = Modifier
				.fillMaxWidth()
				.height(34.dp)
				.clip(
					RoundedCornerShape(
						topStart = 8.dp,
						topEnd = 8.dp,
						bottomStart = 8.dp,
						bottomEnd = 8.dp
					)
				)
				.background(Color.Transparent)
				.border(
					1.dp,
					Color(
						red = 0.18039216101169586f,
						green = 0.4431372582912445f,
						blue = 0.9882352948188782f,
						alpha = 1f
					),
					RoundedCornerShape(
						topStart = 8.dp,
						topEnd = 8.dp,
						bottomStart = 8.dp,
						bottomEnd = 8.dp
					)
				)
				.padding(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 8.dp)
				.alpha(1f)
				.clickable {
					collapsePaymentMethods()
				}
		) {
			Text(
				text = stringResource(R.string.cpsdk_text_select_payment_method_collapse),
				textAlign = TextAlign.Start,
				fontSize = 13.sp,
				textDecoration = TextDecoration.None,
				letterSpacing = -0.07999999821186066.sp,
				lineHeight = 18.sp,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier
					.width(60.dp)
					.alpha(1f),
				color = Color(red = 0.18039216101169586f, green = 0.4431372582912445f, blue = 0.9882352948188782f, alpha = 1f),
				fontWeight = FontWeight.Normal,
				fontStyle = FontStyle.Normal,
			)
		}
	}
}
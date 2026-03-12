package ru.cloudpayments.sdk.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import coil.compose.AsyncImage
import ru.cloudpayments.sdk.R
import ru.cloudpayments.sdk.analytics.AnalyticsManager
import ru.cloudpayments.sdk.api.models.SBPBanksItem
import ru.cloudpayments.sdk.api.models.intent.CPPaymentMethod
import ru.cloudpayments.sdk.configuration.CloudpaymentsSDK
import ru.cloudpayments.sdk.databinding.DialogCpsdkComposeBottomSheetFragmentBinding
import ru.cloudpayments.sdk.models.ApiError
import ru.cloudpayments.sdk.ui.PaymentActivity
import ru.cloudpayments.sdk.ui.dialogs.SelectPaymentMethodFragment.ISelectPaymentMethodFragment
import ru.cloudpayments.sdk.ui.dialogs.base.BasePaymentBottomSheetFragment
import ru.cloudpayments.sdk.util.InjectorUtils
import ru.cloudpayments.sdk.viewmodel.PaymentSBPViewModel
import ru.cloudpayments.sdk.viewmodel.PaymentSBPViewState

internal enum class PaymentSBPStatus {
	ListOfBanks,
	RunBankApp,
	AlreadyPaid,
	Failed
}

internal class PaymentSBPFragment :
	BasePaymentBottomSheetFragment<PaymentSBPViewState, PaymentSBPViewModel>() {

	interface IPaymentSBPFragment {
		fun runSBPPay(payUrl: String, transactionUuid: String)
		fun retryPayment()
	}

	companion object {

		fun newInstance() = PaymentSBPFragment().apply {

		}
	}

	private var _binding: DialogCpsdkComposeBottomSheetFragmentBinding? = null

	private val binding get() = _binding!!

	private val listOfBanks by lazy {
		sdkConfiguration?.terminalConfiguration?.banksForSbp
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

		AnalyticsManager.instance.trackEvent(
			name = "events.system",
			parameters = mapOf(
				"eventType" to "Open",
				"screenName" to "/methods/Sbp",
				"methodChosen" to "Sbp"
			)
		)

		return super.onCreateDialog(savedInstanceState)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		_binding = DialogCpsdkComposeBottomSheetFragmentBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private var currentState: PaymentSBPViewState? = null

	override val viewModel: PaymentSBPViewModel by viewModels {
		InjectorUtils.providePaymentSBPViewModelFactory(
			sdkConfiguration?.intentId.toString()
		)
	}

	override fun render(state: PaymentSBPViewState) {
		currentState = state
		updateWith(state.status, state.errorMessage)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		if (savedInstanceState == null) {
			activity().component.inject(viewModel)
		}
	}

	private fun updateWith(status: PaymentSBPStatus, errorCode: String? = null) {

		var status = status

		when (status) {
			PaymentSBPStatus.ListOfBanks -> {
				binding.composeView.apply {
					setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
					setContent {
						SBPView()
					}
				}
			}

			PaymentSBPStatus.RunBankApp -> {
				val listener = requireActivity() as? IPaymentSBPFragment
				val payUrl = viewModel.currentState.payUrl
				val transactionUuid = viewModel.currentState.transactionUuid
				if (payUrl != null && transactionUuid != null) {
					listener?.runSBPPay(payUrl, transactionUuid)
					dismiss()
				}
			}

			PaymentSBPStatus.AlreadyPaid -> {

				val listener = requireActivity() as? ISelectPaymentMethodFragment
				listener?.onAltAlreadyPaid(viewModel.currentState.transactionId)

				dismiss()
			}

			PaymentSBPStatus.Failed -> {
				if (errorCode == ApiError.CODE_ERROR_CONNECTION) {
					val listener = requireActivity() as? PaymentActivity
					listener?.onInternetConnectionError()
				} else {
					val listener = requireActivity() as? ISelectPaymentMethodFragment
					listener?.onAltPayLinkError(viewModel.currentState.transactionId, errorCode)
				}
				dismiss()
			}
		}
	}

	@Composable
	private fun SBPView() {

		val searchQuery = remember { mutableStateOf(TextFieldValue("")) }

		Column(
			modifier = Modifier
				.fillMaxWidth()
				.fillMaxHeight()
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.Start),
				modifier = Modifier
					.fillMaxWidth()
					.height(86.dp)
					.padding(start = 36.dp, top = 20.dp, end = 0.dp, bottom = 20.dp)) {

				Image(
					painterResource(id = R.drawable.cpsdk_ic_sbp),
					contentDescription = null)

				Text(
					text = stringResource(id = R.string.cpsdk_text_sbp_title),
					textAlign = TextAlign.Start,
					fontSize = 16.sp,
					textDecoration = TextDecoration.None,
					letterSpacing = 0.5.sp,
					lineHeight = 22.sp,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier
						.alpha(1f),
					color = Color(red = 0.2666666805744171f, green = 0.3019607961177826f, blue = 0.35686275362968445f, alpha = 1f),
					fontWeight = FontWeight.Normal,
					fontStyle = FontStyle.Normal
				)
			}

			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.Start),
				modifier = Modifier
					.fillMaxWidth()
					.padding(start = 36.dp, top = 0.dp, end = 36.dp, bottom = 20.dp)) {
				SearchView(searchQuery)
			}

			BanksListView(searchQuery = searchQuery)
		}
	}

	@Composable
	fun SearchView(state: MutableState<TextFieldValue>) {
		OutlinedTextField(
			value = state.value,
			onValueChange = { value ->
				state.value = value
			},
			placeholder = {
				Text(stringResource(id = R.string.cpsdk_text_sbp_search_bank))
			},
			modifier = Modifier
				.fillMaxWidth(),
			textStyle = TextStyle(color = Color.Black, fontSize = 18.sp),
			leadingIcon = {
				Icon(
					imageVector = ImageVector.vectorResource(id = R.drawable.cpsdk_ic_search_bank),
					contentDescription = "",
					modifier = Modifier
						.padding(15.dp)
						.size(24.dp)
				)
			},
			singleLine = true,
			colors = OutlinedTextFieldDefaults.colors(
				unfocusedTextColor = Color(red = 0.2666666805744171f, green = 0.3019607961177826f, blue = 0.35686275362968445f, alpha = 1f),
				unfocusedBorderColor = Color(red = 0.604f, green = 0.631f, blue = 0.671f, alpha = 1.0f),
				unfocusedLeadingIconColor = Color(red = 0.604f, green = 0.631f, blue = 0.671f, alpha = 1.0f),
				unfocusedPlaceholderColor = Color(red = 0.604f, green = 0.631f, blue = 0.671f, alpha = 1.0f),
				focusedTextColor = Color(red = 0.2666666805744171f, green = 0.3019607961177826f, blue = 0.35686275362968445f, alpha = 1f),
				focusedBorderColor = Color(
					red = 0.18f,
					green = 0.443f,
					blue = 0.988f,
					alpha = 1.0f
				),
				focusedLeadingIconColor = Color(red = 0.604f, green = 0.631f, blue = 0.671f, alpha = 1.0f),
				focusedPlaceholderColor = Color(red = 0.604f, green = 0.631f, blue = 0.671f, alpha = 1.0f),
				cursorColor = Color(red = 0.2666666805744171f, green = 0.3019607961177826f, blue = 0.35686275362968445f, alpha = 1f)
			),
			shape = RoundedCornerShape(8.dp)
		)
	}

	@Composable
	private fun BanksListView(searchQuery: MutableState<TextFieldValue>) {

		val searchText = searchQuery.value.text.trim()

		val banks = if (searchText.isNullOrBlank()) {
			listOfBanks
		} else {
			listOfBanks?.filter {
				it.bankName?.contains(searchText, true) == true
			}
		}

		if (banks.isNullOrEmpty()) {
			NotFoundBankView()
		}

		LazyColumn(
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 36.dp, top = 0.dp, end = 36.dp, bottom = 28.dp)
		) {
			itemsIndexed(banks?.toList() ?: listOf()) { index, item ->
				BankView(item) {
					if (item.isWebClientActive == true) {
						viewModel.getSbpPayLink(item.webClientUrl.toString())
					} else {
						viewModel.getSbpPayLink(item.schema.toString())
					}
				}
			}
		}
	}

	@Composable
	private fun BankView(bank: SBPBanksItem, onClick: (bank: SBPBanksItem) -> Unit) {

		HorizontalDivider(color = Color(red = 0.886274516582489f, green = 0.9098039269447327f, blue = 0.9372549057006836f, alpha = 1f))

		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
			modifier = Modifier
				.fillMaxWidth()
				.height(56.dp)
				.padding(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 0.dp)
				.alpha(1f)
				.clickable { onClick(bank) }) {

			Box(
				modifier = Modifier
					.width(40.dp)
					.height(36.dp)
					.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
			) {

				AsyncImage(
					model = bank.logoUrl,
					contentDescription = null,
					contentScale = ContentScale.Fit,
					modifier = Modifier
						.size(36.dp)
				)
			}

			bank.bankName?.let {
				Text(
					text = it,
					textAlign = TextAlign.Start,
					fontSize = 18.sp,
					textDecoration = TextDecoration.None,
					letterSpacing = 0.25.sp,
					lineHeight = 24.sp,
					overflow = TextOverflow.Ellipsis,
					color = Color(red = 0.13333334028720856f, green = 0.1764705926179886f, blue = 0.2549019753932953f, alpha = 1f),
					fontWeight = FontWeight.Normal,
					fontStyle = FontStyle.Normal,
					modifier = Modifier
				)
			}
		}
	}

	@Composable
	private fun NotFoundBankView() {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 34.dp)
		) {

			Image(
				painterResource(id = R.drawable.cpsdk_ic_not_found_bank),
				contentDescription = null,
				modifier = Modifier
					.fillMaxWidth()
					.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
			)

			Text(
				text = stringResource(id = R.string.cpsdk_text_sbp_not_found_bank),
				textAlign = TextAlign.Center,
				fontSize = 18.sp,
				textDecoration = TextDecoration.None,
				letterSpacing = 0.15000000596046448.sp,
				lineHeight = 24.sp,
				overflow = TextOverflow.Ellipsis,
				color = Color(red = 0.604f, green = 0.631f, blue = 0.671f, alpha = 1.0f),
				fontWeight = FontWeight.Medium,
				fontStyle = FontStyle.Normal,
				modifier = Modifier
					.fillMaxWidth()
					.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 24.dp)
			)
		}
	}

	@Composable
	private fun NoBanksView() {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 34.dp)
		) {

			Image(
				painterResource(id = R.drawable.cpsdk_ic_sbp),
				contentDescription = null,
				modifier = Modifier
					.fillMaxWidth()
					.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 20.dp)
			)

			Image(
				painterResource(id = R.drawable.cpsdk_ic_no_banks_apps),
				contentDescription = null,
				modifier = Modifier
					.fillMaxWidth()
					.padding(start = 0.dp, top = 32.dp, end = 0.dp, bottom = 0.dp)
			)

			Text(
				text = stringResource(id = R.string.cpsdk_text_sbp_no_banks_apps),
				textAlign = TextAlign.Center,
				fontSize = 20.sp,
				textDecoration = TextDecoration.None,
				letterSpacing = 0.15000000596046448.sp,
				lineHeight = 24.sp,
				overflow = TextOverflow.Ellipsis,
				color = Color(red = 0.10980392247438431f, green = 0.10588235408067703f, blue = 0.12156862765550613f, alpha = 1f),
				fontWeight = FontWeight.Medium,
				fontStyle = FontStyle.Normal,
				modifier = Modifier
					.fillMaxWidth()
					.padding(start = 0.dp, top = 24.dp, end = 0.dp, bottom = 56.dp)
			)

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
							red = 0.18039216101169586f,
							green = 0.4431372582912445f,
							blue = 0.9882352948188782f,
							alpha = 1f
						)
					)
					.padding(start = 16.dp, top = 17.dp, end = 16.dp, bottom = 17.dp)
					.alpha(1f)
					.clickable {
						val listener =
							requireActivity() as? IPaymentSBPFragment
						listener?.retryPayment()
						dismiss()
					}

			) {

				Text(
					text = stringResource(id = R.string.cpsdk_text_process_button_tpay_sberpay_sbp),
					textAlign = TextAlign.Start,
					fontSize = 18.sp,
					textDecoration = TextDecoration.None,
					letterSpacing = 0.25.sp,
					lineHeight = 24.sp,
					overflow = TextOverflow.Ellipsis,
					color = Color(red = 1f, green = 1f, blue = 1f, alpha = 1f),
					fontWeight = FontWeight.Normal,
					fontStyle = FontStyle.Normal,
					modifier = Modifier
						.width(217.dp)
						.alpha(1f)
				)
			}
		}
	}

	override fun onCancel(dialog: DialogInterface) {
		super.onCancel(dialog)

		AnalyticsManager.instance.trackEvent(
			name = "events.system",
			parameters = mapOf(
				"eventType" to "Close",
				"screenName" to "/methods/Sbp",
				"methodChosen" to "Sbp"
			)
		)

		if (paymentConfiguration?.singlePaymentMode == CPPaymentMethod.SBP) {
			activity?.finish()
		} else {
			(activity as PaymentActivity).showPaymentOptions()
		}
	}
}
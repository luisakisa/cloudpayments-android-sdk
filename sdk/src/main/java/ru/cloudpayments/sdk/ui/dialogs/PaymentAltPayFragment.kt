package ru.cloudpayments.sdk.ui.dialogs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import ru.cloudpayments.sdk.R
import ru.cloudpayments.sdk.analytics.AnalyticsManager
import ru.cloudpayments.sdk.databinding.DialogCpsdkComposeDialogBinding
import ru.cloudpayments.sdk.ui.dialogs.base.BasePaymentDialogFragment
import ru.cloudpayments.sdk.util.InjectorUtils
import ru.cloudpayments.sdk.viewmodel.PaymentAltPayViewModel
import ru.cloudpayments.sdk.viewmodel.PaymentAltPayViewState

internal enum class PaymentAltPayStatus {
	InProcess,
	Succeeded,
	Failed;
}

internal enum class PaymentAltPayType {
	TinkoffPay,
	SberPay,
	MirPay,
	Sbp,
	Dolyame
}

internal class PaymentAltPayFragment: BasePaymentDialogFragment<PaymentAltPayViewState, PaymentAltPayViewModel>() {
	interface IPaymentAltPayFragment {
		fun onPaymentAltPaySucceed(transactionId: Long)
		fun onPaymentAltPayFailed(transactionId: Long, reasonCode: String?)
		fun finishPayment()
		fun retryPayment()
	}

	companion object {
		private const val ARG_ALT_PAY_TYPE = "ARG_ALT_PAY_TYPE"
		private const val ARG_PAY_URL = "ARG_PAY_URL"
		private const val ARG_TRANSACTION_UUID = "ARG_TRANSACTION_UUID"

		fun newInstance(type: PaymentAltPayType, payUrl: String, transactionUuid: String) = PaymentAltPayFragment().apply {
			arguments = Bundle()
			arguments?.putSerializable(ARG_ALT_PAY_TYPE, type)
			arguments?.putString(ARG_PAY_URL, payUrl)
			arguments?.putString(ARG_TRANSACTION_UUID, transactionUuid)
		}
	}

	private var _binding: DialogCpsdkComposeDialogBinding? = null

	private val binding get() = _binding!!

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		AnalyticsManager.instance.trackEvent(
			name = "events.system",
			parameters = mapOf(
				"eventType" to "Open",
				"screenName" to "Waiting",
				"cardFieldsCount" to 0,
				"methodChosen" to type.toString()
			)
		)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		_binding = DialogCpsdkComposeDialogBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()
		viewModel.cancelCheckIntentStatus()
		_binding = null
	}

	private var currentState: PaymentAltPayViewState? = null

	private val type by lazy {
		arguments?.getSerializable(ARG_ALT_PAY_TYPE)
	}

	private val payUrl by lazy {
		arguments?.getString(ARG_PAY_URL) ?: ""
	}

	private val transactionUuid by lazy {
		arguments?.getString(ARG_TRANSACTION_UUID) ?: ""
	}

	override val viewModel: PaymentAltPayViewModel by viewModels {
		InjectorUtils.providePaymentTPayViewModelFactory(
			requireActivity().application,
			sdkConfig?.intentId.toString(),
			transactionUuid)
	}

	override fun render(state: PaymentAltPayViewState) {
		currentState = state
		updateWith(state.status, state.errorMessage)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		if (savedInstanceState == null) {
			activity().component.inject(viewModel)
		}
	}

	private fun updateWith(status: PaymentAltPayStatus, error: String? = null) {

		var status = status

		when (status) {
			PaymentAltPayStatus.InProcess -> {
				binding.composeView.apply {
					setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
					setContent {
						ProgressView()
					}
				}

				runBankApp(payUrl)
				viewModel.getIntentStatusWithTimer()
			}

			PaymentAltPayStatus.Succeeded, PaymentAltPayStatus.Failed -> {

				val listener = requireActivity() as? IPaymentAltPayFragment

				dismiss()

				if (status == PaymentAltPayStatus.Succeeded) {
					listener?.onPaymentAltPaySucceed(currentState?.transactionId ?: 0)
				} else {
					listener?.onPaymentAltPayFailed(currentState?.transactionId ?: 0, currentState?.reasonCode)
				}
			}
		}
	}

	private fun runBankApp(qrUrl: String?) {
		try {
			val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrUrl))
			context?.startActivity(intent)
		} catch (e: ActivityNotFoundException) {
				AlertDialog.Builder(requireContext())
					.setMessage(getString(R.string.cpsdk_text_alt_pays_no_bank_app))
					.setPositiveButton(getString(R.string.cpsdk_text_process_button_ok)) { dialog, _ ->
						dialog.dismiss()
						retryPayment()
					}
					.setCancelable(false)
					.show()
		}
	}

	@Composable
	private fun ProgressView() {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.Top),
			modifier = Modifier
				.width(350.dp)
				.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
				.background(Color(red = 1f, green = 1f, blue = 1f, alpha = 1f))
				.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 20.dp)
				.alpha(1f)
		) {
			Box(
				modifier = Modifier
					.width(180.dp)
					.height(180.dp)
					.clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
					.background(Color.Transparent)
					.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
					.alpha(1f)
			) {
				Image(
					painter = painterResource(id = R.drawable.cpsdk_ic_progress),
					contentDescription = "Progress"
				)
			}
			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
				modifier = Modifier
					.fillMaxWidth()
					.height(98.dp)
					.clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
					.background(Color.Transparent)
					.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
					.alpha(1f)
			) {
				when (type) {
					PaymentAltPayType.TinkoffPay -> TPayView()
					PaymentAltPayType.SberPay -> SberPayView()
					PaymentAltPayType.MirPay -> MirPayView()
					PaymentAltPayType.Sbp -> SBPView()
					PaymentAltPayType.Dolyame -> DolyameView()
				}
				Text(
					text = stringResource(R.string.cpsdk_text_alt_pay_description),
					textAlign = TextAlign.Center,
					fontSize = 13.sp,
					textDecoration = TextDecoration.None,
					letterSpacing = -0.07999999821186066.sp,
					lineHeight = 18.sp,
					modifier = Modifier
						.fillMaxWidth()
						.alpha(1f),
					color = Color(red = 0.13333334028720856f, green = 0.1764705926179886f, blue = 0.2549019753932953f, alpha = 0.5400000214576721f),
					fontWeight = FontWeight.Normal,
					fontStyle = FontStyle.Normal,
				)
			}
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
				modifier = Modifier

					.fillMaxWidth()
					.height(56.dp)
					.clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
					.background(Color.Transparent)
					.border(1.dp, Color(red = 0.18039216101169586f, green = 0.4431372582912445f, blue = 0.9882352948188782f, alpha = 1f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
					.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
					.alpha(1f)
					.clickable {

						AnalyticsManager.instance.trackEvent(
							name = "events.action",
							parameters = mapOf(
								"screenName" to "/link/${type.toString()}",
								"methodChosen" to type.toString(),
								"eventType" to "Button",
								"elementLabel" to "ChooseOtherMethod",
								"actionType" to "Click"
							)
						)

						retryPayment()
					}
			) {
				Text(
					text = stringResource(R.string.cpsdk_text_alt_pay_button_select_payment_method),
					textAlign = TextAlign.Center,
					fontSize = 18.sp,
					textDecoration = TextDecoration.None,
					letterSpacing = 0.25.sp,
					lineHeight = 24.sp,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier
						.width(213.dp)
						.alpha(1f),
					color = Color(red = 0.18039216101169586f, green = 0.4431372582912445f, blue = 0.9882352948188782f, alpha = 1f),
					fontWeight = FontWeight.Normal,
					fontStyle = FontStyle.Normal,
				)
			}
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
				modifier = Modifier
					.width(169.dp)
					.height(16.dp)
					.clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
					.background(Color.Transparent)
					.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
					.alpha(1f)
			) {
				Image(
					painter = painterResource(id = R.drawable.cpsdk_footer),
					contentDescription = "Secured by CloudPayments"
				)
			}
		}
	}

	@Composable
	private fun TPayView() {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
			modifier = Modifier
				.width(241.dp)
				.height(30.dp)
				.clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
				.background(Color.Transparent)
				.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
				.alpha(1f)
		) {
			Text(
				text = stringResource(R.string.cpsdk_text_alt_pay_title),
				textAlign = TextAlign.Start,
				fontSize = 20.sp,
				textDecoration = TextDecoration.None,
				letterSpacing = 0.15000000596046448.sp,
				lineHeight = 24.sp,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier
					.width(160.dp)
					.alpha(1f),
				color = Color(red = 0f, green = 0f, blue = 0f, alpha = 1f),
				fontWeight = FontWeight.Medium,
				fontStyle = FontStyle.Normal,
			)
			Box(
				modifier = Modifier
					.width(71.dp)
					.height(30.dp)
					.clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
					.background(Color.Transparent)
					.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp)
					.alpha(1f)
			) {
				Image(
					painter = painterResource(id = R.drawable.cpsdk_alt_tpay_logo),
					contentDescription = "Pay"
				)
			}
		}
	}

	@Composable
	private fun SberPayView() {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
			modifier = Modifier
				.width(241.dp)
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
				.alpha(1f)
		) {
			Text(
				text = stringResource(R.string.cpsdk_text_alt_pay_title),
				textAlign = TextAlign.Start,
				fontSize = 20.sp,
				textDecoration = TextDecoration.None,
				letterSpacing = 0.15000000596046448.sp,
				lineHeight = 24.sp,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier
					.width(160.dp)
					.alpha(1f),
				color = Color(red = 0f, green = 0f, blue = 0f, alpha = 1f),
				fontWeight = FontWeight.Medium,
				fontStyle = FontStyle.Normal,
			)
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
					.alpha(1f)
			) {
				Image(
					painter = painterResource(id = R.drawable.cpsdk_alt_sber_pay_logo),
					contentDescription = "Pay"
				)
			}
		}
	}

	@Composable
	private fun MirPayView() {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
			modifier = Modifier
				.width(241.dp)
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
				.alpha(1f)
		) {
			Text(
				text = stringResource(R.string.cpsdk_text_alt_pay_title),
				textAlign = TextAlign.Start,
				fontSize = 20.sp,
				textDecoration = TextDecoration.None,
				letterSpacing = 0.15000000596046448.sp,
				lineHeight = 24.sp,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier
					.width(160.dp)
					.alpha(1f),
				color = Color(red = 0f, green = 0f, blue = 0f, alpha = 1f),
				fontWeight = FontWeight.Medium,
				fontStyle = FontStyle.Normal,
			)
			Box(
				modifier = Modifier
					.width(76.dp)
					.height(18.dp)
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
				Image(
					painter = painterResource(id = R.drawable.cpsdk_alt_mir_pay_logo),
					contentDescription = "Pay"
				)
			}
		}
	}

	@Composable
	private fun SBPView() {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
			modifier = Modifier
				.width(241.dp)
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
				.alpha(1f)
		) {
			Text(
				text = stringResource(R.string.cpsdk_text_alt_pay_sbp_title),
				textAlign = TextAlign.Start,
				fontSize = 20.sp,
				textDecoration = TextDecoration.None,
				letterSpacing = 0.15000000596046448.sp,
				lineHeight = 24.sp,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier
					.fillMaxWidth()
					.alpha(1f),
				color = Color(red = 0f, green = 0f, blue = 0f, alpha = 1f),
				fontWeight = FontWeight.Medium,
				fontStyle = FontStyle.Normal,
			)
		}
	}

	@Composable
	private fun DolyameView() {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
			modifier = Modifier
				.width(284.dp)
				.height(24.dp)
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
			Text(
				text = stringResource(R.string.cpsdk_text_alt_pay_title),
				textAlign = TextAlign.Start,
				fontSize = 20.sp,
				textDecoration = TextDecoration.None,
				letterSpacing = 0.15000000596046448.sp,
				lineHeight = 24.sp,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier
					.width(160.dp)
					.alpha(1f),
				color = Color(red = 0f, green = 0f, blue = 0f, alpha = 1f),
				fontWeight = FontWeight.Medium,
				fontStyle = FontStyle.Normal,
			)
			Box(
				modifier = Modifier
					.width(114.dp)
					.height(16.dp)
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
				Image(
					painter = painterResource(id = R.drawable.cpsdk_alt_dolyame_logo_vector),
					contentDescription = "Pay"
				)
			}
		}
	}

	private fun retryPayment() {

		val listener = requireActivity() as? IPaymentAltPayFragment

		if (paymentConfiguration?.singlePaymentMode != null) {
				listener?.finishPayment()
				dismiss()
				activity?.finish()
		} else {
			listener?.retryPayment()
			dismiss()
		}
	}
}

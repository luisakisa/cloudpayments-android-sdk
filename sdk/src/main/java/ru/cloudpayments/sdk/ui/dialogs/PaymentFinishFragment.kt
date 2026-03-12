package ru.cloudpayments.sdk.ui.dialogs

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
import ru.cloudpayments.sdk.configuration.CloudpaymentsSDK
import ru.cloudpayments.sdk.databinding.DialogCpsdkComposeDialogBinding
import ru.cloudpayments.sdk.models.ApiError
import ru.cloudpayments.sdk.models.Currency
import ru.cloudpayments.sdk.ui.dialogs.base.BasePaymentDialogFragment
import ru.cloudpayments.sdk.util.InjectorUtils
import ru.cloudpayments.sdk.viewmodel.PaymentFinishViewModel
import ru.cloudpayments.sdk.viewmodel.PaymentFinishViewState

internal enum class PaymentFinishStatus {
	Success,
	AlreadyPaid,
	Error
}

internal class PaymentFinishFragment: BasePaymentDialogFragment<PaymentFinishViewState, PaymentFinishViewModel>() {
	interface IPaymentFinishFragment {
		fun finishPayment()
		fun retryPayment()
	}

	companion object {

		private const val ARG_STATUS = "arg_status"
		private const val ARG_TRANSACTION_ID = "arg_transaction_id"
		private const val ARG_REASON_CODE = "arg_reason_code"
		private const val ARG_RETRY_PAYMENT = "arg_retry_payment"


		fun newInstance(status: PaymentFinishStatus) = PaymentFinishFragment().apply {
			arguments = Bundle()
			arguments?.putString(ARG_STATUS, status.toString())
		}

		fun newInstance(status: PaymentFinishStatus, reasonCode: String, retryPayment: Boolean) = PaymentFinishFragment().apply {
			arguments = Bundle()
			arguments?.putString(ARG_STATUS, status.toString())
			arguments?.putString(ARG_REASON_CODE, reasonCode)
			arguments?.putBoolean(ARG_RETRY_PAYMENT, retryPayment)
		}

		fun newInstance(status: PaymentFinishStatus, transactionId: Long, reasonCode: String, retryPayment: Boolean) = PaymentFinishFragment().apply {
			arguments = Bundle()
			arguments?.putString(ARG_STATUS, status.toString())
			arguments?.putLong(ARG_TRANSACTION_ID, transactionId)
			arguments?.putString(ARG_REASON_CODE, reasonCode)
			arguments?.putBoolean(ARG_RETRY_PAYMENT, retryPayment)
		}
	}

	private var _binding: DialogCpsdkComposeDialogBinding? = null

	private val binding get() = _binding!!

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		if (status == PaymentFinishStatus.Success) {
			AnalyticsManager.instance.trackEvent(
				name = "events.system",
				parameters = mapOf(
					"eventType" to "Open",
					"screenName" to "/success",
					"methodChosen" to AnalyticsManager.instance.getMethodChosen(),
				"context" to transactionId
				)
			)
		} else {
			AnalyticsManager.instance.trackEvent(
				name = "events.system",
				parameters = mapOf(
					"eventType" to "Open",
					"screenName" to "/fail",
					"methodChosen" to AnalyticsManager.instance.getMethodChosen(),
					"context" to reasonCode
				)
			)
		}
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
		_binding = null
	}

	override val viewModel: PaymentFinishViewModel by viewModels {
		InjectorUtils.providePaymentFinishViewModelFactory(
			status,
			transactionId,
			reasonCode)
	}

	override fun render(state: PaymentFinishViewState) {
		updateWith(status)
	}

	private val status by lazy {
		val stringStatus = arguments?.getString(ARG_STATUS) ?: ""
		try {
			PaymentFinishStatus.valueOf(stringStatus)
		} catch(e: IllegalArgumentException) {
			PaymentFinishStatus.Error
		}
	}

	private val transactionId by lazy {
		arguments?.getLong(ARG_TRANSACTION_ID) ?: 0
	}

	private val reasonCode by lazy {
		arguments?.getString(ARG_REASON_CODE) ?: ""
	}

	private val retryPayment by lazy {

		var retry = arguments?.getBoolean(ARG_RETRY_PAYMENT) ?: false

		if (retry && paymentConfiguration?.singlePaymentMode != null) {
			retry = false
		}
		retry
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		if (savedInstanceState == null) {
			activity().component.inject(viewModel)
			updateWith(status)
		}
	}

	private fun updateWith(status: PaymentFinishStatus) {

		val listener = requireActivity() as? IPaymentFinishFragment

		if (paymentConfiguration?.singlePaymentMode != null) {
			if(paymentConfiguration?.showResultScreenForSinglePaymentMode == false) {
				listener?.finishPayment()
				dismiss()
				activity?.finish()
			}
		}

		var status = status

		when (status) {
			PaymentFinishStatus.Success, PaymentFinishStatus.AlreadyPaid -> {
				binding.composeView.apply {
					setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
					setContent {
						SuccessView()
					}
				}
			}

			PaymentFinishStatus.Error -> {
				binding.composeView.apply {
					setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
					setContent {
						ErrorView()
					}
				}
			}
		}
	}

	@Composable
	private fun SuccessView() {
		val listener = requireActivity() as? IPaymentFinishFragment
		val amount = paymentConfiguration?.paymentData?.amount
		val currency = Currency.getSymbol(paymentConfiguration?.paymentData?.currency)
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
					painter = painterResource(id = R.drawable.cpsdk_ic_success),
					contentDescription = "Success"
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
				Text(
					text = if (status == PaymentFinishStatus.AlreadyPaid) stringResource(R.string.cpsdk_text_final_already_paid) else stringResource(R.string.cpsdk_text_final_success),
					textAlign = TextAlign.Center,
					fontSize = 22.sp,
					textDecoration = TextDecoration.None,
					letterSpacing = -0.25999999046325684.sp,
					lineHeight = 28.sp,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier
						.width(250.dp)
						.alpha(1f),
					color = Color(red = 0.13333334028720856f, green = 0.1764705926179886f, blue = 0.2549019753932953f, alpha = 1f),
					fontWeight = FontWeight.SemiBold,
					fontStyle = FontStyle.Normal,
				)
				Text(
					text = stringResource(R.string.cpsdk_text_final_success_extra),
					textAlign = TextAlign.Center,
					fontSize = 13.sp,
					textDecoration = TextDecoration.None,
					letterSpacing = -0.07999999821186066.sp,
					lineHeight = 18.sp,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier
						.fillMaxWidth()
						.alpha(1f),
					color = Color(red = 0.13333334028720856f, green = 0.1764705926179886f, blue = 0.2549019753932953f, alpha = 0.5400000214576721f),
					fontWeight = FontWeight.Normal,
					fontStyle = FontStyle.Normal,
				)
				Text(
					text = "$amount $currency",
					textAlign = TextAlign.Center,
					fontSize = 22.sp,
					textDecoration = TextDecoration.None,
					letterSpacing = -0.25999999046325684.sp,
					lineHeight = 28.sp,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier
						.fillMaxWidth()
						.alpha(1f),
					color = Color(red = 0.13333334028720856f, green = 0.1764705926179886f, blue = 0.2549019753932953f, alpha = 1f),
					fontWeight = FontWeight.SemiBold,
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
					.background(Color(red = 0.18039216101169586f, green = 0.4431372582912445f, blue = 0.9882352948188782f, alpha = 1f))
					.padding(start = 16.dp, top = 17.dp, end = 16.dp, bottom = 17.dp)
					.alpha(1f)
					.clickable {

						AnalyticsManager.instance.trackEvent(
							name = "events.action",
							parameters = mapOf(
								"screenName" to "/success",
								"eventType" to "Button",
								"elementLabel" to "ReturnToShop",
								"actionType" to "Click"
							)
						)

						listener?.finishPayment()
						dismiss()
						activity?.finish()
					}
			) {
				Text(
					text = stringResource(R.string.cpsdk_text_final_close),
					textAlign = TextAlign.Center,
					fontSize = 17.sp,
					textDecoration = TextDecoration.None,
					letterSpacing = -0.4300000071525574.sp,
					lineHeight = 22.sp,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier
						.width(167.dp)
						.alpha(1f),
					color = Color(red = 1f, green = 1f, blue = 1f, alpha = 1f),
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
	private fun ErrorView() {
		val listener = requireActivity() as? IPaymentFinishFragment
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
					painter = painterResource(id = R.drawable.cpsdk_ic_error),
					contentDescription = "Error"
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
				Text(
					text = context?.let { ApiError.getErrorDescription(it, reasonCode) } ?: "",
					textAlign = TextAlign.Center,
					fontSize = 22.sp,
					textDecoration = TextDecoration.None,
					letterSpacing = -0.25999999046325684.sp,
					lineHeight = 28.sp,
					modifier = Modifier
						.fillMaxWidth()
						.alpha(1f),
					color = Color(red = 0.13333334028720856f, green = 0.1764705926179886f, blue = 0.2549019753932953f, alpha = 1f),
					fontWeight = FontWeight.SemiBold,
					fontStyle = FontStyle.Normal,
				)
				Text(
					text = context?.let { ApiError.getErrorDescriptionExtra(it, reasonCode) } ?: "",
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
					.background(Color(red = 0.18039216101169586f, green = 0.4431372582912445f, blue = 0.9882352948188782f, alpha = 1f))
					.padding(start = 16.dp, top = 17.dp, end = 16.dp, bottom = 17.dp)
					.alpha(1f)
					.clickable {
						if (retryPayment) {

							AnalyticsManager.instance.trackEvent(
								name = "events.action",
								parameters = mapOf(
									"screenName" to "/fail",
									"eventType" to "Button",
									"elementLabel" to "Try again",
									"actionType" to "Click"
								)
							)

							listener?.retryPayment()
							dismiss()
						} else {
							listener?.finishPayment()
							dismiss()
							activity?.finish()
						}
					}
			) {
				Text(
					text = if (retryPayment) stringResource(R.string.cpsdk_text_final_retry) else stringResource(R.string.cpsdk_text_final_close) ,
					textAlign = TextAlign.Center,
					fontSize = 17.sp,
					textDecoration = TextDecoration.None,
					letterSpacing = -0.4300000071525574.sp,
					lineHeight = 22.sp,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier
						.width(167.dp)
						.alpha(1f),
					color = Color(red = 1f, green = 1f, blue = 1f, alpha = 1f),
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
}
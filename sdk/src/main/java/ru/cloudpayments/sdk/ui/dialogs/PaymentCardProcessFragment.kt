package ru.cloudpayments.sdk.ui.dialogs

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import com.google.gson.GsonBuilder
import ru.cloudpayments.sdk.R
import ru.cloudpayments.sdk.api.models.intent.CPMDPacket
import ru.cloudpayments.sdk.databinding.DialogCpsdkComposeDialogBinding
import ru.cloudpayments.sdk.ui.dialogs.base.BasePaymentDialogFragment
import ru.cloudpayments.sdk.util.InjectorUtils
import ru.cloudpayments.sdk.viewmodel.PaymentCardProcessViewModel
import ru.cloudpayments.sdk.viewmodel.PaymentCardProcessViewState

internal enum class PaymentCardProcessStatus {
	InProcess,
	Succeeded,
	AlreadyPaid,
	Failed;
}

internal class PaymentCardProcessFragment: BasePaymentDialogFragment<PaymentCardProcessViewState, PaymentCardProcessViewModel>(), IntentApiThreeDsDialogFragment.ThreeDSDialogListener {
	interface IPaymentCardProcessFragment {
		fun onPaymentCardSucceed(transactionId: Long)
		fun onPaymentCardAlreadyPaid(transactionId: Long)
		fun onPaymentCardFailed(transactionId: Long, reasonCode: String?)
	}

	companion object {
		private const val ARG_CRYPTOGRAM = "ARG_CRYPTOGRAM"

		fun newInstance(cryptogram: String) = PaymentCardProcessFragment().apply {
			arguments = Bundle()
			arguments?.putString(ARG_CRYPTOGRAM, cryptogram)
		}
	}

	private var _binding: DialogCpsdkComposeDialogBinding? = null

	private val binding get() = _binding!!

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

	private var currentState: PaymentCardProcessViewState? = null

	override val viewModel: PaymentCardProcessViewModel by viewModels {
		InjectorUtils.providePaymentProcessViewModelFactory(
			sdkConfig?.intentId.toString(),
			paymentConfiguration!!.paymentData,
			cryptogram,
			sdkConfig?.saveCard)
	}

	override fun render(state: PaymentCardProcessViewState) {
		currentState = state
		updateWith(state.status, state.reasonCode)

		if (!state.acsUrl.isNullOrEmpty() && !state.paReq.isNullOrEmpty() && !state.threeDsCallbackId.isNullOrEmpty() && state.transaction?.transactionId != null) {

			val mdPacket = CPMDPacket(state.threeDsCallbackId, state.transaction.transactionId)
			val gson = GsonBuilder().disableHtmlEscaping().create();
			var mdPacketString  = gson.toJson(mdPacket)
			mdPacketString = Base64.encodeToString(mdPacketString.toByteArray(), Base64.NO_WRAP).trim()

			val dialog = IntentApiThreeDsDialogFragment.newInstance(sdkConfig?.intentId.toString(), state.acsUrl, state.paReq, mdPacketString)

			dialog.setTargetFragment(this, 1)
			dialog.show(parentFragmentManager, null)

			viewModel.clearThreeDsData()
		}
	}

	private val cryptogram by lazy {
		arguments?.getString(ARG_CRYPTOGRAM) ?: ""
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		if (savedInstanceState == null) {
			activity().component.inject(viewModel)

			updateWith(PaymentCardProcessStatus.InProcess)
			viewModel.pay()
		}
	}

	private fun updateWith(status: PaymentCardProcessStatus, errorCode: String? = null) {

		var status = status

		when (status) {
			PaymentCardProcessStatus.InProcess -> {
				binding.composeView.apply {
					setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
					setContent {
						ProgressView()
					}
				}
			}

			PaymentCardProcessStatus.Succeeded, PaymentCardProcessStatus.AlreadyPaid, PaymentCardProcessStatus.Failed -> {

				val listener = requireActivity() as? IPaymentCardProcessFragment

				dismiss()

				if (status == PaymentCardProcessStatus.Succeeded) {
					listener?.onPaymentCardSucceed(currentState?.transactionId ?: 0)
				} else if (status == PaymentCardProcessStatus.AlreadyPaid) {
					listener?.onPaymentCardAlreadyPaid(currentState?.transactionId ?: 0)
				} else{
					listener?.onPaymentCardFailed(currentState?.transactionId ?: 0, errorCode)
				}
			}
		}
	}

	override fun onAuthorizationCompleted() {
		updateWith(PaymentCardProcessStatus.Succeeded)
	}

	override fun onAuthorizationFailed(error: String) {
		updateWith(PaymentCardProcessStatus.Failed, error)
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
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
					modifier = Modifier
						.fillMaxWidth()
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
						text = stringResource(R.string.cpsdk_text_process_title),
						textAlign = TextAlign.Start,
						fontSize = 20.sp,
						textDecoration = TextDecoration.None,
						letterSpacing = 0.15000000596046448.sp,
						lineHeight = 24.sp,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier
							.wrapContentSize()
							.alpha(1f),
						color = Color(red = 0f, green = 0f, blue = 0f, alpha = 1f),
						fontWeight = FontWeight.Medium,
						fontStyle = FontStyle.Normal,
					)
				}
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
package ru.cloudpayments.sdk.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import ru.cloudpayments.sdk.Constants
import ru.cloudpayments.sdk.R
import ru.cloudpayments.sdk.analytics.AnalyticsManager
import ru.cloudpayments.sdk.analytics.models.AnalyticsClientMeta
import ru.cloudpayments.sdk.analytics.network.AnalyticsRepository
import ru.cloudpayments.sdk.api.CloudPaymentsIntentApi
import ru.cloudpayments.sdk.api.CloudpaymentsApi
import ru.cloudpayments.sdk.api.models.intent.CPPaymentMethod
import ru.cloudpayments.sdk.api.models.intent.CPPostIntentRequestBody
import ru.cloudpayments.sdk.api.models.intent.CPPostIntentResponse
import ru.cloudpayments.sdk.api.models.intent.CPTerminalFeatures
import ru.cloudpayments.sdk.api.models.intent.PayerInfo
import ru.cloudpayments.sdk.configuration.CloudpaymentsSDK
import ru.cloudpayments.sdk.configuration.EmailBehavior
import ru.cloudpayments.sdk.configuration.PaymentConfiguration
import ru.cloudpayments.sdk.dagger2.CloudpaymentsComponent
import ru.cloudpayments.sdk.dagger2.CloudpaymentsModule
import ru.cloudpayments.sdk.dagger2.CloudpaymentsNetModule
import ru.cloudpayments.sdk.dagger2.DaggerCloudpaymentsComponent
import ru.cloudpayments.sdk.databinding.ActivityCpsdkPaymentBinding
import ru.cloudpayments.sdk.log.CloudPaymentsSendLogHttpClient
import ru.cloudpayments.sdk.log.CloudPaymentsUncaughtExceptionHandler
import ru.cloudpayments.sdk.models.ApiError
import ru.cloudpayments.sdk.models.SDKConfiguration
import ru.cloudpayments.sdk.ui.dialogs.PaymentAltPayFragment
import ru.cloudpayments.sdk.ui.dialogs.PaymentAltPayType
import ru.cloudpayments.sdk.ui.dialogs.PaymentCardFragment
import ru.cloudpayments.sdk.ui.dialogs.PaymentCardProcessFragment
import ru.cloudpayments.sdk.ui.dialogs.PaymentFinishFragment
import ru.cloudpayments.sdk.ui.dialogs.PaymentFinishStatus
import ru.cloudpayments.sdk.ui.dialogs.PaymentSBPFragment
import ru.cloudpayments.sdk.ui.dialogs.SelectPaymentMethodFragment
import ru.cloudpayments.sdk.ui.dialogs.base.BasePaymentBottomSheetFragment
import ru.cloudpayments.sdk.util.GooglePayHandler
import ru.cloudpayments.sdk.util.customSort
import ru.cloudpayments.sdk.util.isEmailValid
import ru.cloudpayments.sdk.util.getCpSdkHost
import ru.cloudpayments.sdk.util.getUUID
import ru.cloudpayments.sdk.util.nextFragment
import javax.inject.Inject

internal class PaymentActivity : FragmentActivity(),
								 BasePaymentBottomSheetFragment.IPaymentFragment,
								 SelectPaymentMethodFragment.ISelectPaymentMethodFragment,
								 PaymentCardFragment.IPaymentCardFragment,
								 PaymentCardProcessFragment.IPaymentCardProcessFragment,
								 PaymentSBPFragment.IPaymentSBPFragment,
								 PaymentAltPayFragment.IPaymentAltPayFragment,
								 PaymentFinishFragment.IPaymentFinishFragment {

	val sdkConfiguration: SDKConfiguration = SDKConfiguration()

	private var disposable: Disposable? = null

	@Inject
	lateinit var api: CloudpaymentsApi

	@Inject
	lateinit var intentApi: CloudPaymentsIntentApi

	companion object {
		private const val REQUEST_CODE_GOOGLE_PAY = 1
		private const val EXTRA_CONFIGURATION = "EXTRA_CONFIGURATION"

		fun getStartIntent(context: Context, configuration: PaymentConfiguration): Intent {
			val intent = Intent(context, PaymentActivity::class.java)
			intent.putExtra(EXTRA_CONFIGURATION, configuration)
			return intent
		}
	}

	override fun finish() {
		super.finish()
		overridePendingTransition(R.anim.cpsdk_fade_in, R.anim.cpsdk_fade_out)
	}

	internal val component: CloudpaymentsComponent by lazy {
		DaggerCloudpaymentsComponent
			.builder()
			.cloudpaymentsModule(CloudpaymentsModule())
			.cloudpaymentsNetModule(
				CloudpaymentsNetModule(
					paymentConfiguration!!.publicId
				)
			)
			.build()
	}

	val paymentConfiguration by lazy {
		intent.getParcelableExtra<PaymentConfiguration>(EXTRA_CONFIGURATION)
	}

	private lateinit var binding: ActivityCpsdkPaymentBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		Thread.setDefaultUncaughtExceptionHandler(CloudPaymentsUncaughtExceptionHandler.getInstance(this))
		super.onCreate(savedInstanceState)

		Log.e("HOST", getCpSdkHost() ?: "NO")

		CloudPaymentsSendLogHttpClient.setPublicId(paymentConfiguration?.publicId.toString())

		initAnalytics()

		AnalyticsManager.instance.trackEvent(
			name = "events.system",
			parameters = mapOf(
				"eventType" to "Open",
				"screenName" to "PreStart",
				"cardFieldsCount" to 0
			)
		)

		binding = ActivityCpsdkPaymentBinding.inflate(layoutInflater)
		val view = binding.root
		setContentView(view)

		component.inject(this)

		checkCurrency()

		getPublicKey()
	}

	private fun initAnalytics() {

		val repository = AnalyticsRepository()

		val clientMeta = AnalyticsClientMeta(
			sessionId = getUUID(),
			deviceId = ru.cloudpayments.sdk.util.getDeviceId(this),
			sessionStartTime = System.currentTimeMillis(),
			screenWidth = resources.displayMetrics.widthPixels,
			screenHeight = resources.displayMetrics.heightPixels,
			pageUrl = "Android SDK"
		)

		AnalyticsManager.initialize(
			project = "cloud.payments.mpf",
			repository = repository,
			clientMeta = clientMeta
		)
	}

	override fun onDestroy() {
		super.onDestroy()

		disposable?.dispose()
		disposable = null

		AnalyticsManager.instance.trackEvent(
			name = "events.action",
			parameters = mapOf(
				"actionType" to "Close",
				"elementLabel" to "Close",
				"elementType" to "Modal",
				"screenName" to "/methods"
			)
		)

		AnalyticsManager.clear()
	}

	private fun getPublicKey() {
		disposable = api.getPublicKey()
			.toObservable()
			.observeOn(AndroidSchedulers.mainThread())
			.map { response ->
				sdkConfiguration.publicKey.pem = response.pem
				sdkConfiguration.publicKey.version = response.version

				postIntent()
			}
			.onErrorReturn {
				onInternetConnectionError()
			}
			.subscribe()
	}

	private fun postIntent() {

		val paymentSchema = if (paymentConfiguration?.useDualMessagePayment == true)  "Dual" else "Single"

		val payer =  paymentConfiguration?.paymentData?.payer
		val payerInfo = PayerInfo(
			accountId =  paymentConfiguration?.paymentData?.accountId,
			email = paymentConfiguration?.paymentData?.email,
			firstName = payer?.firstName,
			lastName = payer?.lastName,
			middleName = payer?.middleName,
			birthDay = payer?.birthDay,
			address = payer?.address,
			street = payer?.street,
			city = payer?.city,
			country = payer?.country,
			phone = payer?.phone,
			postcode = payer?.postcode
		)


		var metaData: JsonObject? = null

		try {
			val jsonElement = JsonParser.parseString(paymentConfiguration?.paymentData?.jsonData)
			metaData = jsonElement.asJsonObject
		} catch (e: Exception) {
			Log.e("CP SDK", "JSON ERROR: " + e.message)
		}


		val body = CPPostIntentRequestBody(
			publicTerminalId = paymentConfiguration?.publicId.toString(),
			amount = paymentConfiguration?.paymentData?.amount?.toDouble() ?: 0.0,
			currency = paymentConfiguration?.paymentData?.currency ?: "RUB",
			externalId = paymentConfiguration?.paymentData?.externalId,
			description =  paymentConfiguration?.paymentData?.description,
			receiptEmail =  paymentConfiguration?.paymentData?.email,
			cpUserInfo = payerInfo,
			paymentSchema = paymentSchema,
			receipt = paymentConfiguration?.paymentData?.receipt,
			recurrent = paymentConfiguration?.paymentData?.recurrent,
			paymentMethodSequence = paymentConfiguration?.paymentMethodSequence,
			metadata = metaData,
			successRedirectUrl = Constants.schemaForDeeplinkToSdk + getCpSdkHost(),
			failRedirectUrl = Constants.schemaForDeeplinkToSdk + getCpSdkHost())

		disposable = intentApi.postIntent(body)
			.toObservable()
			.observeOn(AndroidSchedulers.mainThread())
			.map { response ->
				if (response.code() == 200 || response.code() == 201) {
					response.body()?.let { saveIntentData(it) }
				} else {

					AnalyticsManager.instance.trackEvent(
						name = "events.system",
						parameters = mapOf(
							"eventType" to "Open",
							"screenName" to "/static-error",
							"cardFieldsCount" to 0,
							"methodChosen" to null
						)
					)

					incorrectConfiguration()
				}
			}
			.onErrorReturn { error ->
				onInternetConnectionError()
			}
			.subscribe()
	}

	private fun saveIntentData(paymentIntent: CPPostIntentResponse) {

		sdkConfiguration.secret = paymentIntent.secret
		sdkConfiguration.intentId = paymentIntent.id

		for (paymentMethod in paymentIntent.paymentMethods!!) {

			paymentMethod.type?.let { type ->

				when (paymentMethod.type) {
					CPPaymentMethod.CARD,
					CPPaymentMethod.T_PAY,
					CPPaymentMethod.SBER_PAY,
					CPPaymentMethod.SBP,
					CPPaymentMethod.DOLYAME,
					CPPaymentMethod.MIR_PAY ->
						sdkConfiguration.availablePaymentMethods.add(type)
				}


				if (paymentMethod.type == CPPaymentMethod.SBP) {
					paymentMethod.banks?.let { banks ->
						sdkConfiguration.terminalConfiguration.banksForSbp = banks
					}
				}
			}
		}

		sdkConfiguration.availablePaymentMethods =
			customSort(sdkConfiguration.availablePaymentMethods, paymentIntent.paymentMethodSequence!!) as ArrayList<String>

		sdkConfiguration.terminalConfiguration.saveCardMode =
			paymentIntent.terminalInfo?.features?.saveCardMode.toString()

		sdkConfiguration.terminalConfiguration.isCvvRequired =
			paymentIntent.terminalInfo?.isCvvRequired

		sdkConfiguration.terminalConfiguration.isAllowedNotSanctionedCards =
			paymentIntent.terminalInfo?.features?.isAllowedNotSanctionedCards

		sdkConfiguration.terminalConfiguration.isQiwi =
			paymentIntent.terminalInfo?.features?.isQiwi

		sdkConfiguration.terminalConfiguration.skipExpiryValidation =
			paymentIntent.terminalInfo?.skipExpiryValidation

		startAnalytics(paymentIntent)

		if (paymentConfiguration?.singlePaymentMode != null) {

			val singlePaymentMode = if (paymentConfiguration?.singlePaymentMode in sdkConfiguration.availablePaymentMethods) paymentConfiguration?.singlePaymentMode else ""

			runSinglePaymentMethod(singlePaymentMode)
		} else {
			if (sdkConfiguration.availablePaymentMethods.size == 1) {
				if (paymentConfiguration?.emailBehavior != EmailBehavior.REQUIRED) {
					runSinglePaymentMethod(sdkConfiguration.availablePaymentMethods[0])
				} else if (isEmailValid(paymentConfiguration?.paymentData?.email)) {
					runSinglePaymentMethod(sdkConfiguration.availablePaymentMethods[0])
				} else {
					showSelectPaymentMethodFragment()
				}
			} else {
				showSelectPaymentMethodFragment()
			}
		}
	}

	private fun startAnalytics(paymentIntent: CPPostIntentResponse) {
		AnalyticsManager.instance.sendUserProperties(
			userProperties = mapOf(
				"project" to "mobileSDK", // Constant for SDK
				"region" to "RU",
				"publicId" to  paymentConfiguration?.publicId,
				"intentId" to sdkConfiguration.intentId,
				"terminalIsTest" to paymentIntent.terminalInfo?.isTest,
				"saveCard" to paymentIntent.tokenize,
				"forceSaveCard" to paymentIntent.terminalInfo?.features?.saveCardMode.equals(
					CPTerminalFeatures.SAVE_CARD_FORCE),
				"sendToEmail" to !paymentConfiguration?.paymentData?.email.isNullOrEmpty(),
				"forceSendToEmail" to paymentConfiguration?.emailBehavior?.equals(EmailBehavior.REQUIRED),
				"isRecurring" to (paymentConfiguration?.paymentData?.recurrent != null),
				"isStatic" to  false // Constant for SDK
			)
		)

		AnalyticsManager.instance.trackEvent(
			name = "events.system",
			parameters = mapOf(
				"eventType" to "Open",
				"screenName" to "Start",
				"cardFieldsCount" to 0,
				"methodsAvailable" to sdkConfiguration.availablePaymentMethods
			)
		)
	}

	private fun showSelectPaymentMethodFragment() {
		binding.layoutProgress.isVisible = false
		val fragment = SelectPaymentMethodFragment.newInstance()
		fragment.show(supportFragmentManager, "")
	}

	private fun runSinglePaymentMethod(paymentMethod: String?) {

		binding.layoutProgress.isVisible = false
		when (paymentMethod) {
			CPPaymentMethod.CARD -> runCardPayment()
			CPPaymentMethod.SBP -> runSbp()
			CPPaymentMethod.T_PAY -> getAltPayLink(CPPaymentMethod.T_PAY)
			CPPaymentMethod.MIR_PAY -> getAltPayLink(CPPaymentMethod.MIR_PAY)
			CPPaymentMethod.SBER_PAY -> getAltPayLink(CPPaymentMethod.SBER_PAY)
			CPPaymentMethod.DOLYAME -> getAltPayLink(CPPaymentMethod.DOLYAME)
			else -> incorrectConfiguration()
		}
	}

	fun getAltPayLink(paymentMethod: String) {

		Log.e("SET_ALT", "getAltPayLink")

		AnalyticsManager.instance.setMethodChosen(paymentMethod)
		Log.e("SET_ALT", AnalyticsManager.instance.getMethodChosen())

		val transactionUuid = getUUID()

		disposable = intentApi.getAltPayLink(sdkConfiguration.intentId, paymentMethod, transactionUuid)
			.toObservable()
			.observeOn(AndroidSchedulers.mainThread())
			.map { response ->
				if (response.code() == 200 && response.body() != null && response.body()!!.isNotEmpty()) {
					when (paymentMethod) {
						CPPaymentMethod.T_PAY -> runTPay(response.body().toString(), transactionUuid)
						CPPaymentMethod.MIR_PAY -> runMirPay(response.body().toString(), transactionUuid)
						CPPaymentMethod.SBER_PAY -> runSberPay(response.body().toString(), transactionUuid)
						CPPaymentMethod.DOLYAME -> runDolyame(response.body().toString(), transactionUuid)
					}
				} else {
					incorrectConfiguration()
				}
			}
			.onErrorReturn {
				onInternetConnectionError()
			}
			.subscribe()
	}

	@Deprecated("Deprecated in Java")
	override fun onBackPressed() {
		val fragment = supportFragmentManager.findFragmentById(R.id.frame_content)
		if (fragment is BasePaymentBottomSheetFragment<*, *>) {
			fragment.handleBackButton()
		} else {
			super.onBackPressed()
		}
	}

	override fun runCardPayment() {

		AnalyticsManager.instance.setMethodChosen(CPPaymentMethod.CARD)

		AnalyticsManager.instance.trackEvent(
			name = "events.action",
			parameters = mapOf(
				"screenName" to "/methods",
				"eventType" to "Button",
				"elementLabel" to "CardPay",
				"actionType" to "Click"
			)
		)

		val fragment = PaymentCardFragment.newInstance()
		fragment.show(supportFragmentManager, "")
	}

	override fun runTPay(payUrl: String, transactionUuid: String) {

		AnalyticsManager.instance.setMethodChosen(CPPaymentMethod.T_PAY)

		AnalyticsManager.instance.trackEvent(
			name = "events.action",
			parameters = mapOf(
				"screenName" to "/methods",
				"eventType" to "Button",
				"elementLabel" to "TPay",
				"actionType" to "Click"
			)
		)

		val fragment = PaymentAltPayFragment.newInstance(PaymentAltPayType.TinkoffPay, payUrl, transactionUuid)
		fragment.show(supportFragmentManager, "")
	}

	override fun runSberPay(payUrl: String, transactionUuid: String) {

		AnalyticsManager.instance.setMethodChosen(CPPaymentMethod.SBER_PAY)

		AnalyticsManager.instance.trackEvent(
			name = "events.action",
			parameters = mapOf(
				"screenName" to "/methods",
				"eventType" to "Button",
				"elementLabel" to "SberPay",
				"actionType" to "Click"
			)
		)

		val fragment = PaymentAltPayFragment.newInstance(PaymentAltPayType.SberPay, payUrl, transactionUuid)
		fragment.show(supportFragmentManager, "")
	}

	override fun runMirPay(deepLink: String, transactionUuid: String) {

		AnalyticsManager.instance.setMethodChosen(CPPaymentMethod.MIR_PAY)

		AnalyticsManager.instance.trackEvent(
			name = "events.action",
			parameters = mapOf(
				"screenName" to "/methods",
				"eventType" to "Button",
				"elementLabel" to "MirPay",
				"actionType" to "Click"
			)
		)

		val fragment = PaymentAltPayFragment.newInstance(PaymentAltPayType.MirPay, deepLink, transactionUuid)
		fragment.show(supportFragmentManager, "")
	}

	override fun runDolyame(payUrl: String, transactionUuid: String) {

		AnalyticsManager.instance.setMethodChosen(CPPaymentMethod.DOLYAME)

		AnalyticsManager.instance.trackEvent(
			name = "events.action",
			parameters = mapOf(
				"screenName" to "/methods",
				"eventType" to "Button",
				"elementLabel" to "Dolyame",
				"actionType" to "Click"
			)
		)

		val fragment = PaymentAltPayFragment.newInstance(PaymentAltPayType.Dolyame, payUrl, transactionUuid)
		fragment.show(supportFragmentManager, "")
	}

	override fun runSbp() {

		AnalyticsManager.instance.setMethodChosen(CPPaymentMethod.SBP)

		AnalyticsManager.instance.trackEvent(
			name = "events.action",
			parameters = mapOf(
				"screenName" to "/methods",
				"eventType" to "Button",
				"elementLabel" to "Sbp",
				"actionType" to "Click"
			)
		)

		val fragment = PaymentSBPFragment.newInstance()
		fragment.show(supportFragmentManager, "")
	}

	override fun runSBPPay(payUrl: String, transactionUuid: String) {
		val fragment = PaymentAltPayFragment.newInstance(PaymentAltPayType.Sbp, payUrl, transactionUuid)
		fragment.show(supportFragmentManager, "")
	}

	override fun onAltPayLinkError(transactionId: Long?, errorCode: String?) {
		onPaymentAltPayFailed(transactionId ?: 0, "")
	}

	override fun runGooglePay() {

		AnalyticsManager.instance.setMethodChosen(CPPaymentMethod.GOOGLE_PAY)

		sdkConfiguration.terminalConfiguration.gPayGatewayName
		GooglePayHandler.present(paymentConfiguration!!, sdkConfiguration.terminalConfiguration.gPayGatewayName, this, REQUEST_CODE_GOOGLE_PAY)
	}

	override fun onAltAlreadyPaid(transactionId: Long?) {
		setResult(RESULT_OK, Intent().apply {
			putExtra(CloudpaymentsSDK.IntentKeys.TransactionId.name, transactionId)
			putExtra(
				CloudpaymentsSDK.IntentKeys.TransactionStatus.name,
				CloudpaymentsSDK.TransactionStatus.Succeeded
			)
		})

		val fragment = PaymentFinishFragment.newInstance(PaymentFinishStatus.AlreadyPaid)
		fragment.show(supportFragmentManager, "")
	}

	override fun onPayCardClicked(cryptogram: String) {

		AnalyticsManager.instance.trackEvent(
			name = "events.action",
			parameters = mapOf(
				"screenName" to "/methods/card-edit",
				"methodChosen" to "Card",
				"eventType" to "Button",
				"elementLabel" to "PayByCard",
				"actionType" to "Click",
				"context" to "Valid"
			)
		)

		val fragment = PaymentCardProcessFragment.newInstance(cryptogram)
		fragment.show(supportFragmentManager, "")
	}

	override fun onPaymentCardSucceed(transactionId: Long) {
		setResult(RESULT_OK, Intent().apply {
			putExtra(CloudpaymentsSDK.IntentKeys.TransactionId.name, transactionId)
			putExtra(
				CloudpaymentsSDK.IntentKeys.TransactionStatus.name,
				CloudpaymentsSDK.TransactionStatus.Succeeded
			)
		})

		val fragment = PaymentFinishFragment.newInstance(PaymentFinishStatus.Success)
		fragment.show(supportFragmentManager, "")
	}

	override fun onPaymentCardAlreadyPaid(transactionId: Long) {
		setResult(RESULT_OK, Intent().apply {
			putExtra(CloudpaymentsSDK.IntentKeys.TransactionId.name, transactionId)
			putExtra(
				CloudpaymentsSDK.IntentKeys.TransactionStatus.name,
				CloudpaymentsSDK.TransactionStatus.Succeeded
			)
		})

		val fragment = PaymentFinishFragment.newInstance(PaymentFinishStatus.AlreadyPaid)
		fragment.show(supportFragmentManager, "")
	}

	override fun onPaymentCardFailed(transactionId: Long, reasonCode: String?) {
		setResult(RESULT_OK, Intent().apply {
			putExtra(CloudpaymentsSDK.IntentKeys.TransactionId.name, transactionId)
			putExtra(
				CloudpaymentsSDK.IntentKeys.TransactionStatus.name,
				CloudpaymentsSDK.TransactionStatus.Failed
			)
			reasonCode?.let { putExtra(CloudpaymentsSDK.IntentKeys.TransactionReasonCode.name, it) }
		})

		val fragment = PaymentFinishFragment.newInstance(
			PaymentFinishStatus.Error,
			transactionId,
			reasonCode ?: "",
			true
		)
		fragment.show(supportFragmentManager, "")
	}

	override fun onPaymentAltPaySucceed(transactionId: Long) {
		setResult(RESULT_OK, Intent().apply {
			putExtra(CloudpaymentsSDK.IntentKeys.TransactionId.name, transactionId)
			putExtra(
				CloudpaymentsSDK.IntentKeys.TransactionStatus.name,
				CloudpaymentsSDK.TransactionStatus.Succeeded
			)
		})

		val fragment = PaymentFinishFragment.newInstance(PaymentFinishStatus.Success)
		fragment.show(supportFragmentManager, "")
	}

	override fun onPaymentAltPayFailed(transactionId: Long, reasonCode: String?) {
		setResult(RESULT_OK, Intent().apply {
			putExtra(CloudpaymentsSDK.IntentKeys.TransactionId.name, transactionId)
			putExtra(
				CloudpaymentsSDK.IntentKeys.TransactionStatus.name,
				CloudpaymentsSDK.TransactionStatus.Failed
			)
			reasonCode?.let {
				putExtra(CloudpaymentsSDK.IntentKeys.TransactionReasonCode.name, it)
			}
		})

		val fragment = PaymentFinishFragment.newInstance(
			PaymentFinishStatus.Error,
			transactionId,
			reasonCode ?: "",
			true
		)
		fragment.show(supportFragmentManager, "")
	}

	fun onInternetConnectionError() {
		binding.layoutProgress.isVisible = false
		val fragment = PaymentFinishFragment.newInstance(
			PaymentFinishStatus.Error,
			ApiError.CODE_ERROR_CONNECTION,
			false
		)
		fragment.show(supportFragmentManager, "")
	}

	override fun finishPayment() {
		finish()
	}

	override fun retryPayment() {
		setResult(RESULT_CANCELED, Intent())
		showPaymentOptions()
	}

	fun showPaymentOptions() {
		showSelectPaymentMethodFragment()
	}

	override fun paymentWillFinish() {
		finish()
	}

	@Deprecated("Deprecated in Java")
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

		when (requestCode) {

			REQUEST_CODE_GOOGLE_PAY -> {
				when (resultCode) {
					RESULT_OK -> {
						handleGooglePaySuccess(data)
					}

					RESULT_CANCELED, AutoResolveHelper.RESULT_ERROR -> {
						handleGooglePayFailure(data)
					}

					else -> super.onActivityResult(requestCode, resultCode, data)
				}
			}

			else -> super.onActivityResult(requestCode, resultCode, data)
		}
	}

	private fun handleGooglePaySuccess(intent: Intent?) {
		if (intent != null) {
			val paymentData = PaymentData.getFromIntent(intent)
			val token = paymentData?.paymentMethodToken?.token

			if (token != null) {
				val fragment = PaymentCardProcessFragment.newInstance(token)
				nextFragment(fragment, true, R.id.frame_content)
			}
		}
	}

	private fun handleGooglePayFailure(intent: Intent?) {
		finish()
	}

	private fun checkCurrency() {
		if (paymentConfiguration!!.paymentData.currency.isEmpty()) {
			paymentConfiguration!!.paymentData.currency = "RUB"
		}
	}

	private fun incorrectConfiguration() {
		Toast.makeText(this, "Incorrect configuration", Toast.LENGTH_SHORT).show()
		finish()
	}

}
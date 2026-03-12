package ru.cloudpayments.sdk.ui.dialogs

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.cloudpayments.sdk.Constants
import ru.cloudpayments.sdk.analytics.AnalyticsManager
import ru.cloudpayments.sdk.databinding.DialogCpsdkThreeDsBinding
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class IntentApiThreeDsDialogFragment : DialogFragment() {
	interface ThreeDSDialogListener {
		fun onAuthorizationCompleted()
		fun onAuthorizationFailed(errorCode: String)
	}

	companion object {
		private const val ARG_INTENT_ID = "intent_id"
		private const val ARG_ACS_URL = "acs_url"
		private const val ARG_MD = "md"
		private const val ARG_PA_REQ = "pa_req"

		fun newInstance(intentId: String, acsUrl: String, paReq: String, md: String) = IntentApiThreeDsDialogFragment().apply {
			arguments = Bundle().also {
				it.putString(ARG_INTENT_ID, intentId)
				it.putString(ARG_ACS_URL, acsUrl)
				it.putString(ARG_MD, md)
				it.putString(ARG_PA_REQ, paReq)
			}
		}
	}

	private var _binding: DialogCpsdkThreeDsBinding? = null

	private val binding get() = _binding!!


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		AnalyticsManager.instance.trackEvent(
			name = "events.system",
			parameters = mapOf(
				"eventType" to "Open",
				"screenName" to "/3ds",
				"cardFieldsCount" to 1,
				"methodChosen" to "Card"
			)
		)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		_binding = DialogCpsdkThreeDsBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private val intentId by lazy {
		requireArguments().getString(ARG_INTENT_ID) ?: ""
	}

	private val acsUrl by lazy {
		requireArguments().getString(ARG_ACS_URL) ?: ""
	}

	private val md by lazy {
		requireArguments().getString(ARG_MD) ?: ""
	}

	private val paReq by lazy {
		requireArguments().getString(ARG_PA_REQ) ?: ""
	}

	private var listener: ThreeDSDialogListener? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		isCancelable = false

		binding.webView.webViewClient = ThreeDsWebViewClient()
		binding.webView.settings.domStorageEnabled = true
		binding.webView.settings.javaScriptEnabled = true
		binding.webView.settings.javaScriptCanOpenWindowsAutomatically = true
		binding.webView.addJavascriptInterface(ThreeDsJavaScriptInterface(), "JavaScriptThreeDs")

		val termUrl = "${Constants.baseIntentApiUrl}api/intent/${intentId}/threeDsResult"

		try {
			val params = StringBuilder()
				.append("PaReq=").append(URLEncoder.encode(paReq, "UTF-8"))
				.append("&MD=").append(URLEncoder.encode(md, "UTF-8"))
				.append("&TermUrl=").append(URLEncoder.encode(termUrl, "UTF-8"))
				.toString()
			binding.webView.postUrl(acsUrl, params.toByteArray())
		} catch (e: UnsupportedEncodingException) {
			e.printStackTrace()
		}

		binding.icClose.setOnClickListener {
			listener?.onAuthorizationFailed("0")
			dismiss()
		}
	}

	override fun onStart() {
		super.onStart()
		val window = dialog!!.window
		window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
	}

	private inner class ThreeDsWebViewClient : WebViewClient() {
		override fun onPageFinished(view: WebView, url: String) {
			val termUrl = "${Constants.baseIntentApiUrl}api/intent/${intentId}/threeDsResult"
			if (url.startsWith(termUrl, true)) {
				view.isGone = true
				view.loadUrl("javascript:window.JavaScriptThreeDs.processHTML(document.getElementsByTagName('html')[0].innerHTML);")
			}
		}
	}

	internal inner class ThreeDsJavaScriptInterface {
		@JavascriptInterface
		fun processHTML(html: String?) {

			Log.e("3DS", html.toString())

			val doc: Document = Jsoup.parse(html ?: "")
			val element: Element? = doc.select("pre").first()
			val jsonString = element?.ownText()

			val gson = Gson()
			val response = gson.fromJson(jsonString, ThreeDsResponse::class.java)
			requireActivity().runOnUiThread {
				if (response.success && response.data.success) {
					listener?.onAuthorizationCompleted()
				} else {
					val errorCode = if (response.data.code.startsWith("R")) response.data.code else "R${response.data.code}"

					listener?.onAuthorizationFailed(errorCode)
				}
				dismissAllowingStateLoss()
			}
		}

	}

	override fun onAttach(context: Context) {
		super.onAttach(context)

		listener = targetFragment as? ThreeDSDialogListener
		if (listener == null) {
			listener = context as? ThreeDSDialogListener
		}
	}

	@Deprecated("Deprecated in Java")
	override fun onAttach(activity: Activity) {
		super.onAttach(activity)

		listener = targetFragment as? ThreeDSDialogListener
		if (listener == null) {
			listener = activity as? ThreeDSDialogListener
		}
	}

	data class ThreeDsResponse(
		@SerializedName("data") val data: ThreeDsResponseData,
		@SerializedName("errorMessage") val errorMessage: String?,
		@SerializedName("success") val success: Boolean
	)

	data class ThreeDsResponseData(
		@SerializedName("success") val success: Boolean,
		@SerializedName("code") val code: String
	)
}

package ru.cloudpayments.sdk

class Constants {
	companion object {
		const val baseApiUrl = "https://api.cloudpayments.ru/"
		const val baseIntentApiUrl = "https://intent-api.cloudpayments.ru/"
//		const val baseApiUrl = "https://api-preprod.cloudpayments.ru/"
//		const val baseIntentApiUrl = "https://intent-api-preprod.cloudpayments.ru/"

		const val fromtMonitoringApiUrl = "https://fm.cloudpayments.ru/"
		const val fromtMonitoringApiKey = "f35c3bcc-1621-408b-b92b-b0b2fc92f63c"
		const val disableFrontMonitoring = false

		const val schemaForDeeplinkToSdk = "cloudpayments://"

		const val userAgent = "Mozilla/5.0 (Linux; Android 16; sdk_gphone64_arm64 Build/BP22.250221.010; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/135.0.7049.38 Mobile Safari/537.36"
	}
}
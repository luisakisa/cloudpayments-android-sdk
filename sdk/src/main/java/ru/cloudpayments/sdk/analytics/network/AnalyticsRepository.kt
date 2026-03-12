package ru.cloudpayments.sdk.analytics.network

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import ru.cloudpayments.sdk.analytics.models.AnalyticsEvent


class AnalyticsRepository {

	companion object {
		private const val STATIST_URL = "https://api-statist.tinkoff.ru/gateway/v1/events"
	}

	private val client: OkHttpClient

	private val json = Json {
		prettyPrint = false
		ignoreUnknownKeys = true
	}

	init {
		val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor()
			.setLevel(HttpLoggingInterceptor.Level.BODY)
		client = OkHttpClient.Builder()
			.addNetworkInterceptor(interceptor)
			.build()
	}

	fun send(events: List<AnalyticsEvent>) {
		val jsonBody = json.encodeToString(
			ListSerializer(AnalyticsEvent.serializer()), events
		)
		val request = Request.Builder()
			.url(STATIST_URL)
			.post(jsonBody.toRequestBody("application/json".toMediaType()))
			.build()

		client.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw IllegalStateException("Failed to send events: ${response.code}")
			}
		}
	}
}
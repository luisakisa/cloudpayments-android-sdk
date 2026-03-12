package ru.cloudpayments.sdk.analytics.models

import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsClientMeta(
	val sessionId: String,
	val deviceId: String,
	val sessionStartTime: Long,
	val screenWidth: Int? = null,
	val screenHeight: Int? = null,
	val sdkVersion: String = "",
	val codegenVersion: String = "",
	val utmSource: String? = null,
	val pageUrl: String = ""
)
package ru.cloudpayments.sdk.analytics.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AnalyticsEvent(
	val kind: String? = null,
	val project: String,
	val name: String? = null,
	val parameters: JsonObject? = null,
	val eventParameters: AnalyticsEventMeta,
	val clientParameters: AnalyticsClientMeta,
	val userProperties: JsonObject? = null,
	val ABGroups: JsonObject? = null
)
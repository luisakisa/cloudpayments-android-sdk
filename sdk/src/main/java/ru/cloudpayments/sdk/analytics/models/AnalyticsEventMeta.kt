package ru.cloudpayments.sdk.analytics.models

import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsEventMeta(
	val clientEventTimestamp: Long,
	val clientUploadTimestamp: Long,
	val sequence: Int,
	val uuid: String
)
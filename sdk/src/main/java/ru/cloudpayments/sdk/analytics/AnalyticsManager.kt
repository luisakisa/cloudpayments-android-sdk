package ru.cloudpayments.sdk.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import ru.cloudpayments.sdk.analytics.models.AnalyticsClientMeta
import ru.cloudpayments.sdk.analytics.models.AnalyticsEvent
import ru.cloudpayments.sdk.analytics.models.AnalyticsEventMeta
import ru.cloudpayments.sdk.analytics.network.AnalyticsRepository
import java.util.UUID

class AnalyticsManager(
	private val project: String,
	private val repository: AnalyticsRepository,
	private val clientMeta: AnalyticsClientMeta
) {

	private val scope = CoroutineScope(Dispatchers.IO)
	private var sequence = 0
	private var methodChosen = ""

	companion object {
		@Volatile
		private var _instance: AnalyticsManager? = null

		val instance: AnalyticsManager
			get() = _instance ?: throw IllegalStateException(
				"AnalyticsManager is not initialized. Call initialize() first."
			)

		fun initialize(
			project: String,
			repository: AnalyticsRepository,
			clientMeta: AnalyticsClientMeta
		) {
			if (_instance == null) {
				synchronized(this) {
					if (_instance == null) {
						_instance = AnalyticsManager(project, repository, clientMeta)
					}
				}
			}
		}

		fun clear() {
			synchronized(this) {
				_instance = null
			}
		}
	}

	fun trackEvent(
		name: String,
		parameters: Map<String, Any?> = emptyMap()
	) {
		val event = AnalyticsEvent(
			project = project,
			name = name,
			parameters = parameters.toJsonObject(),
			eventParameters = AnalyticsEventMeta(
				clientEventTimestamp = System.currentTimeMillis(),
				clientUploadTimestamp = System.currentTimeMillis(),
				sequence = ++sequence,
				uuid = UUID.randomUUID().toString()
			),
			clientParameters = clientMeta
		)

		scope.launch {
			repository.send(listOf(event))
		}
	}

	fun sendUserProperties(
		userProperties: Map<String, Any?> = emptyMap()
	) {

		val parameters =
			buildJsonObject {
				put("\$set", userProperties.toJsonObject())
			}

		val event = AnalyticsEvent(
			project = project,
			kind = "userProperties",
			eventParameters = AnalyticsEventMeta(
				clientEventTimestamp = System.currentTimeMillis(),
				clientUploadTimestamp = System.currentTimeMillis(),
				sequence = ++sequence,
				uuid = UUID.randomUUID().toString()
			),
			clientParameters = clientMeta,
			userProperties = parameters
		)

		scope.launch {
			repository.send(listOf(event))
		}
	}

	fun setMethodChosen(method: String) {
		methodChosen = method
	}

	fun getMethodChosen(): String {
		return methodChosen
	}

	private fun Map<String, Any?>.toJsonObject(): JsonObject {
		return buildJsonObject {
			forEach { (key, value) ->
				when (value) {
					is String -> put(key, JsonPrimitive(value))
					is Number -> put(key, JsonPrimitive(value))
					is Boolean -> put(key, JsonPrimitive(value))
					is List<*> -> put(key, JsonArray(value.map { JsonPrimitive(it.toString()) }))
					else -> Unit
				}
			}
		}
	}
}
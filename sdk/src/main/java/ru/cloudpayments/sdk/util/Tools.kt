package ru.cloudpayments.sdk.util

import android.content.Context
import android.content.pm.PackageManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import java.security.MessageDigest
import java.util.*

open class TextWatcherAdapter: TextWatcher {
	override fun afterTextChanged(s: Editable?) {}

	override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}

	override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
}

fun isEmailValid(email: String?): Boolean {
	if (email.isNullOrBlank()) {
		return false
	}
	return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

fun checkAndGetCorrectJsonDataString(json: String?): String? {
	return try {
		val parser = JsonParser()
		val jsonElement = parser.parse(json)
		Gson().toJson(jsonElement)
	} catch (e: JsonSyntaxException) {
		Log.e("CloudPaymentsSDK", "JsonSyntaxException in JsonData")
		null
	} catch (e: NullPointerException) {
		Log.e("CloudPaymentsSDK", "NullPointerException in JsonData")
		null
	}
}

fun getRussianLocale() = Locale("ru", "RU")

fun getSha512(input: String): String {
	return MessageDigest.getInstance("SHA-512")
		.digest(input.toByteArray())
		.joinToString(separator = "") {
			((it.toInt() and 0xff) + 0x100)
				.toString(16)
				.substring(1)
		}
}

fun getUUID() : String {
	return UUID.randomUUID().toString()
}

fun customSort(A: List<String>, B: ArrayList<String>): List<String> {
	// сначала элементы из B, если они есть в A
	val result = mutableListOf<String>()
	result.addAll(B.filter { it in A })
	// потом остальные элементы A
	result.addAll(A.filter { it !in B })
	return result
}

fun getDeviceId(context: Context): String {

	val PREFS_NAME = "analytics_prefs"
	val KEY_DEVICE_ID = "device_id"

	val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
	var id = prefs.getString(KEY_DEVICE_ID, null)

	if (id == null) {
		id = UUID.randomUUID().toString()
		prefs.edit().putString(KEY_DEVICE_ID, id).apply()
	}

	return id
}

fun Context.getCpSdkHost(): String? {
	val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
	return appInfo.metaData?.getString("cp_sdk_host")
}
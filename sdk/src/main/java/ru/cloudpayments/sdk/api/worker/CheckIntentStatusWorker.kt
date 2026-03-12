package ru.cloudpayments.sdk.api.worker

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import ru.cloudpayments.sdk.Constants
import ru.cloudpayments.sdk.api.CloudPaymentsIntentApi
import ru.cloudpayments.sdk.api.CloudPaymentsIntentApiService
import ru.cloudpayments.sdk.api.UserAgentInterceptor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class CheckIntentStatusWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

	companion object {

		const val ARG_INTENT_ID = "arg_intent_id"
		const val ARG_INTENT = "arg_intent"
	}

	lateinit var intentApi: CloudPaymentsIntentApi

	private var disposable: Disposable? = null

	override fun doWork(): Result {

		val future = CompletableFuture<Result>()

		val okHttpClientBuilder = OkHttpClient.Builder()
			.addInterceptor(
				HttpLoggingInterceptor()
					.setLevel(HttpLoggingInterceptor.Level.BODY)
			)
			.addInterceptor(UserAgentInterceptor(Constants.userAgent))

		val client = okHttpClientBuilder
			.connectTimeout(64, TimeUnit.SECONDS)
			.readTimeout(64, TimeUnit.SECONDS)
			.followRedirects(false)
			.build()

		val apiUrl = Constants.baseIntentApiUrl

		val retrofit = Retrofit.Builder()
			.baseUrl(apiUrl)
			.addConverterFactory(GsonConverterFactory.create())
			.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
			.client(client)
			.build()

		intentApi =
			CloudPaymentsIntentApi(retrofit.create(CloudPaymentsIntentApiService::class.java))

		val intentId = inputData.getString(ARG_INTENT_ID) ?: return Result.failure()

		disposable = intentApi.getIntentStatus(intentId)
			.toObservable()
			.observeOn(AndroidSchedulers.mainThread())
			.map { response ->
				val intent = response.body()
				intent?.let {
					val gson = Gson()
					val json = gson.toJson(intent)
					val outputData = Data.Builder()
						.putString(ARG_INTENT, json)
						.build()
					future.complete(Result.success(outputData))
				} ?: run {
					future.complete(Result.failure())
				}
			}
			.onErrorReturn {
				future.complete(Result.failure())
			}
			.subscribe()
		return try { // Блокируем поток, пока не получим результат
			future.get()
		} catch (e: Exception) {
			Result.failure()
		}
	}
}

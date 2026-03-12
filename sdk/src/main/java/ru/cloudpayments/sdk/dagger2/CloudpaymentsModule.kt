package ru.cloudpayments.sdk.dagger2

import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import ru.cloudpayments.sdk.Constants
import ru.cloudpayments.sdk.api.AuthenticationInterceptor
import ru.cloudpayments.sdk.api.CloudPaymentsIntentApi
import ru.cloudpayments.sdk.api.CloudPaymentsIntentApiService
import ru.cloudpayments.sdk.api.CloudpaymentsApi
import ru.cloudpayments.sdk.api.CloudpaymentsApiService
import ru.cloudpayments.sdk.api.UserAgentInterceptor
import ru.cloudpayments.sdk.log.CloudPaymentsLoggingInterceptor
import ru.cloudpayments.sdk.ui.PaymentActivity
import ru.cloudpayments.sdk.viewmodel.PaymentAltPayViewModel
import ru.cloudpayments.sdk.viewmodel.PaymentCardViewModel
import ru.cloudpayments.sdk.viewmodel.PaymentFinishViewModel
import ru.cloudpayments.sdk.viewmodel.SelectPaymentMethodViewModel
import ru.cloudpayments.sdk.viewmodel.PaymentCardProcessViewModel
import ru.cloudpayments.sdk.viewmodel.PaymentSBPViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class CloudpaymentsModule {
	@Provides
	@Singleton
	fun provideRepository(apiService: CloudpaymentsApiService)
			= CloudpaymentsApi(apiService)

	@Provides
	@Singleton
	fun provideIntentRepository(apiService: CloudPaymentsIntentApiService)
			= CloudPaymentsIntentApi(apiService)
}

@Module
class CloudpaymentsNetModule(private val publicId: String) {
	@Provides
	@Singleton
	fun providesHttpLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor()
		.setLevel(HttpLoggingInterceptor.Level.BODY)

	@Provides
	@Singleton
	fun providesAuthenticationInterceptor(): AuthenticationInterceptor
			= AuthenticationInterceptor(publicId)

	@Provides
	@Singleton
	fun providesUserAgentInterceptor(): UserAgentInterceptor
			= UserAgentInterceptor(Constants.userAgent)

	@Provides
	@Singleton
	fun provideOkHttpClientBuilder(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient.Builder
			= OkHttpClient.Builder()
		.addInterceptor(loggingInterceptor)

	@Provides
	@Singleton
	fun provideApiService(okHttpClientBuilder: OkHttpClient.Builder, userAgentInterceptor: UserAgentInterceptor,
						  authenticationInterceptor: AuthenticationInterceptor): CloudpaymentsApiService {
		val client = okHttpClientBuilder
			.addInterceptor(userAgentInterceptor)
			.addInterceptor(authenticationInterceptor)
			.addNetworkInterceptor(CloudPaymentsLoggingInterceptor())
			.connectTimeout(64, TimeUnit.SECONDS)
			.readTimeout(64, TimeUnit.SECONDS)
			.writeTimeout(64, TimeUnit.SECONDS)
			.followRedirects(false)
			.build()

		val apiUrl = Constants.baseApiUrl

		val retrofit = Retrofit.Builder()
			.baseUrl(apiUrl)
			.addConverterFactory(GsonConverterFactory.create())
			.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
			.client(client)
			.build()

		return retrofit.create(CloudpaymentsApiService::class.java)
	}

	@Provides
	@Singleton
	fun provideIntentApiService(okHttpClientBuilder: OkHttpClient.Builder, userAgentInterceptor: UserAgentInterceptor): CloudPaymentsIntentApiService {
		val client = okHttpClientBuilder
			.addInterceptor(userAgentInterceptor)
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

		return retrofit.create(CloudPaymentsIntentApiService::class.java)
	}
}

@Singleton
@Component(modules = [CloudpaymentsModule::class, CloudpaymentsNetModule::class])
internal interface CloudpaymentsComponent {
	fun inject(paymentActivity: PaymentActivity)
	fun inject(optionsViewModel: SelectPaymentMethodViewModel)
	fun inject(cardViewModel: PaymentCardViewModel)
	fun inject(processViewModel: PaymentCardProcessViewModel)
	fun inject(tPayViewModel: PaymentAltPayViewModel)
	fun inject(sbpViewModel: PaymentSBPViewModel)
	fun inject(finishViewModel: PaymentFinishViewModel)
}

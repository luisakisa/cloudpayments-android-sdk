package ru.cloudpayments.sdk.util

import android.app.Application
import ru.cloudpayments.sdk.configuration.PaymentConfiguration
import ru.cloudpayments.sdk.configuration.PaymentData
import ru.cloudpayments.sdk.ui.dialogs.PaymentFinishStatus
import ru.cloudpayments.sdk.viewmodel.PaymentFinishViewModelFactory
import ru.cloudpayments.sdk.viewmodel.SelectPaymentMethodViewModelFactory
import ru.cloudpayments.sdk.viewmodel.PaymentCardProcessViewModelFactory
import ru.cloudpayments.sdk.viewmodel.PaymentSBPViewModelFactory
import ru.cloudpayments.sdk.viewmodel.PaymentAltPayViewModelFactory

internal object InjectorUtils {

    fun provideSelectPaymentMethodViewModelFactory(paymentConfiguration: PaymentConfiguration): SelectPaymentMethodViewModelFactory {
        return SelectPaymentMethodViewModelFactory(paymentConfiguration)
    }
    fun providePaymentProcessViewModelFactory(intentId: String, paymentData: PaymentData, cryptogram: String, saveCard: Boolean?): PaymentCardProcessViewModelFactory {
        return PaymentCardProcessViewModelFactory(intentId, paymentData, cryptogram, saveCard)
    }

    fun providePaymentTPayViewModelFactory(application: Application, intentId: String, transactionUuid: String): PaymentAltPayViewModelFactory {
        return PaymentAltPayViewModelFactory(application, intentId, transactionUuid)
    }

    fun providePaymentSBPViewModelFactory(intentId: String): PaymentSBPViewModelFactory {
        return PaymentSBPViewModelFactory(intentId)
    }

    fun providePaymentFinishViewModelFactory(status: PaymentFinishStatus,
                                             transactionId: Long?,
                                             reasonCode: String?): PaymentFinishViewModelFactory {
        return PaymentFinishViewModelFactory(status,
                                             transactionId,
                                             reasonCode)
    }
}
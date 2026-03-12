package ru.cloudpayments.sdk.viewmodel

import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import ru.cloudpayments.sdk.api.CloudPaymentsIntentApi
import ru.cloudpayments.sdk.api.models.SBPBanksItem
import ru.cloudpayments.sdk.api.models.intent.CPPatchIntentItemRequestBody
import ru.cloudpayments.sdk.api.models.intent.CPPaymentMethod
import ru.cloudpayments.sdk.configuration.PaymentConfiguration
import ru.cloudpayments.sdk.models.ApiError
import ru.cloudpayments.sdk.ui.dialogs.SelectPaymentMethodStatus
import ru.cloudpayments.sdk.util.getUUID
import javax.inject.Inject

internal class SelectPaymentMethodViewModel(
    private val paymentConfiguration: PaymentConfiguration
): BaseViewModel<SelectPaymentMethodViewState>() {
    override var currentState = SelectPaymentMethodViewState()
    override val viewState: MutableLiveData<SelectPaymentMethodViewState> by lazy {
        MutableLiveData(currentState)
    }

    private var disposable: Disposable? = null

    @Inject
    lateinit var intentApi: CloudPaymentsIntentApi

    fun patchIntentAndRunPay(paymentMethod: String, secret: String, intentId: String, email: String? = null, saveCard: Boolean? = null) {

        val state = if (paymentMethod == CPPaymentMethod.CARD) {
            currentState.copy(status = SelectPaymentMethodStatus.CardLoading)
        } else if (paymentMethod == CPPaymentMethod.T_PAY) {
            currentState.copy(status = SelectPaymentMethodStatus.TPayLoading)
        } else if (paymentMethod == CPPaymentMethod.SBER_PAY) {
            currentState.copy(status = SelectPaymentMethodStatus.SberPayLoading)
        } else if (paymentMethod == CPPaymentMethod.SBP) {
            currentState.copy(status = SelectPaymentMethodStatus.SbpLoading)
        } else if (paymentMethod == CPPaymentMethod.MIR_PAY) {
            currentState.copy(status = SelectPaymentMethodStatus.MirPayLoading)
        } else if (paymentMethod == CPPaymentMethod.DOLYAME) {
            currentState.copy(status = SelectPaymentMethodStatus.DolyameLoading)
        } else {
            currentState.copy(status = SelectPaymentMethodStatus.Failed)
        }

        stateChanged(state)

        val body = ArrayList<CPPatchIntentItemRequestBody>()

        if (email!= null) {
            body.add(CPPatchIntentItemRequestBody("/receiptEmail", email))
        }

        if (saveCard!= null) {
            body.add(CPPatchIntentItemRequestBody("/tokenize", saveCard))
        }

        if (body.isEmpty()) {
            if (paymentMethod == CPPaymentMethod.CARD) {
                val state = currentState.copy(status = SelectPaymentMethodStatus.CardSuccess)
                stateChanged(state)
            } else if (paymentMethod == CPPaymentMethod.T_PAY) {
                getTPayPayLink(intentId)
            } else if (paymentMethod == CPPaymentMethod.SBER_PAY) {
                getSberPayPayLink(intentId)
            } else if (paymentMethod == CPPaymentMethod.SBP) {
                val state = currentState.copy(status = SelectPaymentMethodStatus.SbpSuccess)
                stateChanged(state)
            } else if (paymentMethod == CPPaymentMethod.MIR_PAY) {
                getMirPayDeepLink(intentId)
            } else if (paymentMethod == CPPaymentMethod.DOLYAME) {
                getDolyamePayLink(intentId)
            }
            return
        }

        disposable = intentApi.patchIntent(secret, intentId, body)
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { response ->
                if (response.code() == 200) {
                    if (paymentMethod == CPPaymentMethod.CARD) {
                        val state = currentState.copy(status = SelectPaymentMethodStatus.CardSuccess)
                        stateChanged(state)
                    } else if (paymentMethod == CPPaymentMethod.T_PAY) {
                        getTPayPayLink(intentId)
                    } else if (paymentMethod == CPPaymentMethod.SBER_PAY) {
                        getSberPayPayLink(intentId)
                    } else if (paymentMethod == CPPaymentMethod.SBP) {
                        val state = currentState.copy(status = SelectPaymentMethodStatus.SbpSuccess)
                        stateChanged(state)
                    } else if (paymentMethod == CPPaymentMethod.MIR_PAY) {
                        getMirPayDeepLink(intentId)
                    } else if (paymentMethod == CPPaymentMethod.DOLYAME) {
                        getDolyamePayLink(intentId)
                    }
                } else {
                    val state = currentState.copy(status = SelectPaymentMethodStatus.Failed)
                    stateChanged(state)
                }
            }
            .onErrorReturn {
                val state = currentState.copy(status = SelectPaymentMethodStatus.Failed, reasonCode = ApiError.CODE_ERROR_CONNECTION)
                stateChanged(state)
            }
            .subscribe()
    }

    fun getTPayPayLink(intentId: String) {

        val state = currentState.copy(status = SelectPaymentMethodStatus.TPayLoading)
        stateChanged(state)

        val transactionUuid = getUUID()

        disposable = intentApi.getAltPayLink(intentId, CPPaymentMethod.T_PAY, transactionUuid)
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { response ->
                val state = if (response.code() == 200 && response.body() != null && response.body()!!.isNotEmpty()) {
                    currentState.copy(status = SelectPaymentMethodStatus.TPaySuccess,
                                      payUrl = response.body(),
                                      transactionUuid = transactionUuid)
                } else if (response.code() == 409) {
                    currentState.copy(status = SelectPaymentMethodStatus.AlreadyPaid)
                } else {
                    currentState.copy(status = SelectPaymentMethodStatus.Failed)
                }
                stateChanged(state)
            }
            .onErrorReturn {
                val state = currentState.copy(status = SelectPaymentMethodStatus.Failed, reasonCode = ApiError.CODE_ERROR_CONNECTION)
                stateChanged(state)
            }
            .subscribe()
    }

    fun getSberPayPayLink(intentId: String) {

        val state = currentState.copy(status = SelectPaymentMethodStatus.SberPayLoading)
        stateChanged(state)

        val transactionUuid = getUUID()

        disposable = intentApi.getAltPayLink(intentId, CPPaymentMethod.SBER_PAY, transactionUuid)
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { response ->
                val state = if (response.code() == 200 && response.body() != null && response.body()!!.isNotEmpty()) {
                    currentState.copy(status = SelectPaymentMethodStatus.SberPaySuccess,
                                      payUrl = response.body(),
                                      transactionUuid = transactionUuid)
                } else if (response.code() == 409) {
                    currentState.copy(status = SelectPaymentMethodStatus.AlreadyPaid)
                } else {
                    currentState.copy(status = SelectPaymentMethodStatus.Failed)
                }
                stateChanged(state)
            }
            .onErrorReturn {
                val state = currentState.copy(status = SelectPaymentMethodStatus.Failed, reasonCode = ApiError.CODE_ERROR_CONNECTION)
                stateChanged(state)
            }
            .subscribe()
    }

    fun getMirPayDeepLink(intentId: String) {

        val state = currentState.copy(status = SelectPaymentMethodStatus.MirPayLoading)
        stateChanged(state)

        val transactionUuid = getUUID()

        disposable = intentApi.getMirPayDeepLink(intentId, transactionUuid)
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { response ->
                val state = if (response.code() == 200 && response.body() != null && response.body()!!.isNotEmpty()) {
                    currentState.copy(status = SelectPaymentMethodStatus.MirPaySuccess,
                                      payUrl = response.body(),
                                      transactionUuid = transactionUuid)
                } else if (response.code() == 409) {
                    currentState.copy(status = SelectPaymentMethodStatus.AlreadyPaid)
                } else {
                    currentState.copy(status = SelectPaymentMethodStatus.Failed)
                }
                stateChanged(state)
            }
            .onErrorReturn {
                val state = currentState.copy(status = SelectPaymentMethodStatus.Failed, reasonCode = ApiError.CODE_ERROR_CONNECTION)
                stateChanged(state)
            }
            .subscribe()
    }

    fun getDolyamePayLink(intentId: String) {

        val state = currentState.copy(status = SelectPaymentMethodStatus.DolyameLoading)
        stateChanged(state)

        val transactionUuid = getUUID()

        disposable = intentApi.getAltPayLink(intentId, CPPaymentMethod.DOLYAME, transactionUuid)
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { response ->
                val state = if (response.code() == 200 && response.body() != null && response.body()!!.isNotEmpty()) {
                    currentState.copy(status = SelectPaymentMethodStatus.DolyameSuccess,
                                      payUrl = response.body(),
                                      transactionUuid = transactionUuid)
                } else if (response.code() == 409) {
                    currentState.copy(status = SelectPaymentMethodStatus.AlreadyPaid)
                } else {
                    currentState.copy(status = SelectPaymentMethodStatus.Failed)
                }
                stateChanged(state)
            }
            .onErrorReturn {
                val state = currentState.copy(status = SelectPaymentMethodStatus.Failed, reasonCode = ApiError.CODE_ERROR_CONNECTION)
                stateChanged(state)
            }
            .subscribe()
    }

    fun expandPaymentMethods() {
        val state = currentState.copy(collapsePaymentMethods = false)
        stateChanged(state)
    }

    fun collapsePaymentMethods() {
        val state = currentState.copy(collapsePaymentMethods = true)
        stateChanged(state)
    }

    private fun stateChanged(viewState: SelectPaymentMethodViewState) {
        currentState = viewState.copy()
        this.viewState.apply {
            value = viewState
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposable?.dispose()
    }
}

internal data class SelectPaymentMethodViewState(
    val status: SelectPaymentMethodStatus = SelectPaymentMethodStatus.Waiting,
    val reasonCode: String? = null,
    val payUrl: String? = null,
    val transactionUuid: String? = null,
    val providerQrId: String? = null,
    val transactionId: Long? = null,
    val listOfBanks: ArrayList<SBPBanksItem>? = null,
    val isSaveCard: Int? = null,
    val mirPayDeepLink: String? = null,
    val mirPayGuid: String? = null,
    val collapsePaymentMethods: Boolean = true
): BaseViewState()
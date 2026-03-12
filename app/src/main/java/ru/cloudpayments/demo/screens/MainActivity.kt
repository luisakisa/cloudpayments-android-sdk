package ru.cloudpayments.demo.screens

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import ru.cloudpayments.demo.R
import ru.cloudpayments.demo.base.BaseActivity
import ru.cloudpayments.demo.databinding.ActivityMainBinding
import ru.cloudpayments.sdk.api.models.PaymentDataPayer
import ru.cloudpayments.sdk.api.models.intent.CPPaymentMethod
import ru.cloudpayments.sdk.api.models.intent.CPRecurrent
import ru.cloudpayments.sdk.configuration.CloudpaymentsSDK
import ru.cloudpayments.sdk.configuration.EmailBehavior
import ru.cloudpayments.sdk.configuration.PaymentConfiguration
import ru.cloudpayments.sdk.configuration.PaymentData


class MainActivity : BaseActivity() {

	private val cpSdkLauncher = CloudpaymentsSDK.getInstance().launcher(this, result = {

		if (it.status != null) {

			val builder: AlertDialog.Builder = AlertDialog.Builder(this)
			builder.setPositiveButton("OK") { dialog, which ->

			}

			if (it.status == CloudpaymentsSDK.TransactionStatus.Succeeded) {
				builder
					.setTitle("Success")
					.setMessage("Transaction ID: ${it.transactionId}")
			} else {
				builder.setTitle("Fail")
				if (it.reasonCode != 0) {
					builder.setMessage("Transaction ID: ${it.transactionId}, reason code: ${it.reasonCode}")
				} else {
					builder.setMessage("Transaction ID: ${it.transactionId}")
				}
			}

			val dialog: AlertDialog = builder.create()
			dialog.show()
		}
	})

	override val layoutId = R.layout.activity_main

	private lateinit var binding: ActivityMainBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityMainBinding.inflate(layoutInflater)
		val view = binding.root
		setContentView(view)

		binding.buttonRunTop.setOnClickListener {
			runCpSdk()
		}

		binding.buttonRun.setOnClickListener {
			runCpSdk()
		}
	}

	private fun runCpSdk() {

		val publicId = binding.editPublicId.text.toString()

		val amount = if (binding.checkboxAmount.isChecked)
			binding.editAmount.text.toString()
		else
			"0"

		val currency = if (binding.checkboxCurrency.isChecked)
			binding.editCurrency.text.toString()
		else
			""

		val invoiceId = if (binding.checkboxInvoiceId.isChecked)
			binding.editInvoiceId.text.toString()
		else
			""

		val description = if (binding.checkboxDescription.isChecked)
			binding.editDescription.text.toString()
		else
			""

		val accountId = if (binding.checkboxAccountId.isChecked)
			binding.editAccountId.text.toString()
		else
			""

		val email = if (binding.checkboxEmail.isChecked)
			binding.editEmail.text.toString()
		else
			""

		val payerFirstName = if (binding.checkboxPayerFirstName.isChecked)
			binding.editPayerFirstName.text.toString()
		else
			""

		val payerLastName = if (binding.checkboxPayerLastName.isChecked)
			binding.editPayerLastName.text.toString()
		else
			""

		val payerMiddleName = if (binding.checkboxPayerMiddleName.isChecked)
			binding.editPayerMiddleName.text.toString()
		else
			""

		val payerBirthDay = if (binding.checkboxPayerBirth.isChecked)
			binding.editPayerBirth.text.toString()
		else
			""

		val payerAddress = if (binding.checkboxPayerAddress.isChecked)
			binding.editPayerAddress.text.toString()
		else
			""

		val payerStreet = if (binding.checkboxPayerStreet.isChecked)
			binding.editPayerStreet.text.toString()
		else
			""

		val payerCity = if (binding.checkboxPayerCity.isChecked)
			binding.editPayerCity.text.toString()
		else
			""

		val payerCountry = if (binding.checkboxPayerCountry.isChecked)
			binding.editPayerCountry.text.toString()
		else
			""

		val payerPhone = if (binding.checkboxPayerPhone.isChecked)
			binding.editPayerPhone.text.toString()
		else
			""

		val payerPostcode = if (binding.checkboxPayerPostCode.isChecked)
			binding.editPayerPostcode.text.toString()
		else
			""
		
		val jsonData = if (binding.checkboxJsonData.isChecked)
			binding.editJsonData.text.toString()
		else
			""

		val isDualMessagePayment = binding.checkboxDualMessagePayment.isChecked

		var payer = PaymentDataPayer()
		payer.firstName = payerFirstName
		payer.lastName = payerLastName
		payer.middleName = payerMiddleName
		payer.birthDay = payerBirthDay
		payer.address = payerAddress
		payer.street = payerStreet
		payer.city = payerCity
		payer.country = payerCountry
		payer.phone = payerPhone
		payer.postcode = payerPostcode

		val receiptItem = mapOf(
			"Label" to description,
			"Price" to 1.95,
			"Quantity" to 1.0,
			"Amount" to 1.95,
			"Vat" to 20,
			"Method" to 0,
			"Object" to 0
		)

		val receiptItems = ArrayList<Map<String, Any>>()
		receiptItems.add(receiptItem)

		val receiptAmounts = mapOf(
			"Electronic" to 1.95,
			"AdvancePayment" to 0.0,
			"Credit" to 0.0,
			"Provision" to 0.0
		)

		val receipt = mapOf(
			"TaxationSystem" to 0,
			"Email" to email,
			"Phone" to payerPhone,
			"isBso" to false,
			"AgentSign" to 0,
			"Amounts" to receiptAmounts,
			"Items" to receiptItems
		)

		val recurrent = CPRecurrent(
			interval = "Month",
			period = 1,
			customerReceipt = receipt,
			amount = 1.95
		)

		val paymentData = PaymentData(
			amount = amount,
			currency = currency,
			externalId = invoiceId,
			description = description,
			accountId = accountId,
			email = email,
			payer = payer,
			//receipt = receipt,
			//recurrent = recurrent,
			jsonData = jsonData
		)

		val paymentMethodSequence = ArrayList<String>()
		paymentMethodSequence.add(CPPaymentMethod.CARD)
		paymentMethodSequence.add(CPPaymentMethod.T_PAY)

		val configuration = PaymentConfiguration(
			publicId = publicId,
			paymentData = paymentData,
			emailBehavior = EmailBehavior.OPTIONAL,
			useDualMessagePayment = isDualMessagePayment,
			paymentMethodSequence = paymentMethodSequence,
			//singlePaymentMode = CPPaymentMethod.T_PAY,
			//showResultScreenForSinglePaymentMode = false,
			testMode = true
		)

		cpSdkLauncher.launch(configuration)
	}
}
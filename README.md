[![](https://jitpack.io/v/ru.cloudpayments.gitpub.integrations.sdk/cloudpayments-android.svg)](https://jitpack.io/#ru.cloudpayments.gitpub.integrations.sdk/cloudpayments-android)

## CloudPayments SDK for Android 

CloudPayments SDK позволяет интегрировать прием платежей в мобильные приложение для платформы Android.

### Требования
Для работы CloudPayments SDK необходим Android версии 6.0 или выше (API level 23)

### Подключение
1. В build.gradle уровня проекта добавить репозиторий Jitpack

```
repositories {
	maven { url 'https://jitpack.io' }
}
```

2. В build.gradle уровня приложения добавьте следующие зависимость указав последнюю доступную версию SDK и запись в Manifest Placeholders:

[![](https://jitpack.io/v/ru.cloudpayments.gitpub.integrations.sdk/cloudpayments-android.svg)](https://jitpack.io/#ru.cloudpayments.gitpub.integrations.sdk/cloudpayments-android)

```
implementation 'ru.cloudpayments.gitpub.integrations.sdk:cloudpayments-android:latest-release

defaultConfig {
	manifestPlaceholders = [
   		cpSdkHost: "придумайте_префикс.ваш_домен.ru" // Укажите здесь, свои уникальные данные (например cpSdkHost: "paymentsdk.cloudpayments.ru"), это необходимо для формирования deeplink, чтобы после оплаты корректно сработал возврат в SDK из приложения банка. Если вы используете только оплату картой, можно указать пустую строку (cpSdkHost: "")  	]
}
```


### Структура проекта:

* **app** - Пример реализации приложения с использованием SDK
* **sdk** - Исходный код SDK


### Возможности CloudPayments SDK:

Вы можете использовать SDK одним из четырех способов: 

* использовать стандартную платежную форму CloudPayments
* использовать стандартную платежную форму CloudPayments в режиме одного способа оплаты
* реализовать свою платежную форму с использованием функций CloudPaymentsApi без вашего сервера
* реализовать свою платежную форму, сформировать криптограмму и отправить ее в CloudPayments через свой сервер

### Использование стандартной платежной формы CloudPayments:

1.	Создайте CpSdkLauncher для получения результата через Activity Result API (рекомендуется использовать, но если хотите получить результат в onActivityResult этот шаг можно пропустить)

```
val cpSdkLauncher = CloudpaymentsSDK.getInstance().launcher(this, result = {
	if (it.status != null) {
		if (it.status == CloudpaymentsSDK.TransactionStatus.Succeeded) {
			Toast.makeText(this, "Успешно! Транзакция №${it.transactionId}", Toast.LENGTH_SHORT).show()
			CartManager.getInstance()?.clear()
			finish()
		} else {
			if (it.reasonCode != 0) {
				Toast.makeText(this, "Ошибка! Транзакция №${it.transactionId}. Код ошибки ${it.reasonCode}", Toast.LENGTH_SHORT).show()
			} else {
				Toast.makeText(this, "Ошибка! Транзакция №${it.transactionId}.", Toast.LENGTH_SHORT).show()
			}
		}
	}
})
```

2. Создайте вспомогательные объекты и объект PaymentData, передайте через них всю необходимую информацию о платеже.

2.1. Дополнительная информация о плательщике:

```
var payer = PaymentDataPayer() 
	payer.firstName = payerFirstName // Имя
	payer.lastName = payerLastName // Фамилия
	payer.middleName = payerMiddleName // Отчество
	payer.birthDay = payerBirthDay // День рождения
	payer.address = payerAddress // Адрес
	payer.street = payerStreet // Улица
	payer.city = payerCity // Город
	payer.country = payerCountry // Страна
	payer.phone = payerPhone // Телефон
	payer.postcode = payerPostcode // Почтовый индекс
```
 	
2.2. Создание чека (за более подробной информацией по формированию чека, можно обратиться в [документацию CloudKassir](https://developers.cloudkassir.ru/))

**Внимание из-за частых изменений в формате чека, он передается как Map\<String, Any\>**

```
// Создайте позиции для чека
val receiptItem = mapOf(
	"Label" to description,
	"Price" to 1.95,
	"Quantity" to 1.0,
	"Amount" to 1.95,
	"Vat" to 20,
	"Method" to 0,
	"Object" to 0
)
	
// Добавьте позиции для чека в массив
val receiptItems = ArrayList<Map<String, Any>>()
	receiptItems.add(receiptItem)
	
// Дополнительная информация для формирования чека
val receiptAmounts = mapOf(
	"Electronic" to 1.95,
	"AdvancePayment" to 0.0,
	"Credit" to 0.0,
	"Provision" to 0.0
)
	
// Создайте объект чека
val receipt = mapOf(
	"TaxationSystem" to 0,
	"Email" to email,
	"Phone" to payerPhone,
	"isBso" to false,
	"AgentSign" to 0,
	"Amounts" to receiptAmounts,
	"Items" to receiptItems
)
```

2.3. Создание подписки

```
val recurrent = PaymentDataRecurrent(
	interval = "Month",
	period = 1,
	customerReceipt = receipt
)
```

2.4.  Основной объект с данными платежа	
```
val paymentData = PaymentData(
			amount = amount, // Cумма платежа в валюте
			currency = currency, // Валюта
			externalId = invoiceId, // Номер счета или заказа в вашей системе
			description = description, // Описание оплаты в свободной форме
			accountId = accountId, // Идентификатор пользователя
			email = email, // E-mail плательщика, на который будет отправлена квитанция об оплате
			payer = payer, // Информация о плательщике
			receipt = receipt, // Информациюя для создания чека
			recurrent = recurrent, // Инструкции для создания подписки 
			jsonData = jsonData // Любые другие данные, которые будут связаны с транзакцией {name: Ivan}
)
```

3. Создайте объект PaymentConfiguration, передайте в него Public Id из [личного кабинета CloudPayments](https://merchant.cloudpayments.ru/), объект PaymentData, а так же укажите другие параметры.

```
// Если необходимо установите порядок отображения платежных методов:
val paymentMethodSequence = ArrayList<String>()
	paymentMethodSequence.add(CPPaymentMethod.CARD) // 1
	paymentMethodSequence.add(CPPaymentMethod.T_PAY) // 2
// Если указать не все методы, тогда первыми буду отображены переданные методы, а остальные будут отображены в порядке по умолчанию.

val configuration = PaymentConfiguration(
	publicId = publicId, // Ваш PublicID в полученный в ЛК CloudPayments
	paymentData = paymentData, // Информация о платеже
	emailBehavior = EmailBehavior.OPTIONAL, // Опции отображения поля Email (OPTIONAL - пользователь может включить/отключить полеб REQUIRED - поле обязательно для заполнения, HIDDEN - поле не отображается)
	useDualMessagePayment = true // Использовать двухстадийную схему проведения платежа, по умолчанию используется одностадийная схема
	paymentMethodSequence = paymentMethodSequence // Массив с порядком отображения платежных методов
)
```

4. Вызовите форму оплаты. 
```
cpSdkLauncher.launch(configuration) // Если используете Activity Result API

// или

CloudpaymentsSDK.getInstance().start(configuration, this, REQUEST_CODE_PAYMENT) // Если хотите получть результат в onActivityResult 
```

5. Получите результат в onActivityResult (если не используете Activity Result API)
```
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) = when (requestCode) {
		REQUEST_CODE_PAYMENT -> {
			val transactionId = data?.getIntExtra(CloudpaymentsSDK.IntentKeys.TransactionId.name, 0) ?: 0
			val transactionStatus = data?.getSerializableExtra(CloudpaymentsSDK.IntentKeys.TransactionStatus.name) as? CloudpaymentsSDK.TransactionStatus


			if (transactionStatus != null) {
				if (transactionStatus == CloudpaymentsSDK.TransactionStatus.Succeeded) {
					Toast.makeText(this, "Успешно! Транзакция №$transactionId", Toast.LENGTH_SHORT).show()
				} else {
					val reasonCode = data.getIntExtra(CloudpaymentsSDK.IntentKeys.TransactionReasonCode.name, 0) ?: 0
					if (reasonCode > 0) {
						Toast.makeText(this, "Ошибка! Транзакция №$transactionId. Код ошибки $reasonCode", Toast.LENGTH_SHORT).show()
					} else {
						Toast.makeText(this, "Ошибка! Транзакция №$transactionId.", Toast.LENGTH_SHORT).show()
					}
				}
			}
		}
		else -> super.onActivityResult(requestCode, resultCode, data)
```

### Включение T-Pay, SberPay, СБП, МИРPay и Долями в стандартной платежной форме:

1. Включить необходимые опции в [личном кабинете Cloudpayments](https://merchant.cloudpayments.ru/).
2. Если эти опции недоступны, обратитесь к курирующему вас менеджеру.

### Использование стандартной платежной формы CloudPayments в режиме одного способа оплаты:

1. В объекте PaymentConfiguration укажите какой способ оплаты необходимо запустить (singlePaymentMode = CPPaymentMethod.T_PAY), SDK пропустит экран выбора способов оплаты и сразу запустит выбранный способ оплаты (если он доступен на терминале, иначе SDK сообщит об ошибке конфигурации).
2. Если необходимо, скройте финальный экран (showResultScreenForSinglePaymentMode = false) и SDK сразу после оплаты вернет результат работы в ваше приложение, не показывая экран успеха или ошибки, данный параметр работает только в режиме одного способа оплаты (в конфигурации указан singlePaymentMode)
 
```
val configuration = PaymentConfiguration(
			publicId = publicId,
			paymentData = paymentData,
			emailBehavior = EmailBehavior.OPTIONAL,
			useDualMessagePayment = isDualMessagePayment,
			paymentMethodSequence = paymentMethodSequence,
			singlePaymentMode = CPPaymentMethod.T_PAY, // Какой платежный метод необходимо запустить
			showResultScreenForSinglePaymentMode = false // Не показывать финальный экран
		)
```

### Использование вашей платежной формы с использованием функций CloudpaymentsApi:

1. Создайте криптограмму карточных данных

**Для использования нового формата криптограммы:**

1.1. Получите **publicKey** и **keyVersion** в данном методе: [API](https://api.cloudpayments.ru/payments/publickey)

1.2. Используйте полученные **publicKey (Pem)**, **keyVersion (Version)**, а также **merchantPublicId** полученный в [личном кабинете CloudPayments](https://merchant.cloudpayments.ru/) и данные карты для создания криптограммы

```
// Обязательно проверяйте входящие данные карты (номер, срок действия и cvc код) на корректность, иначе метод создания криптограммы вернет null
val cardCryptogram = Card.cardCryptogram(cardNumber, cardExpDate, cardCVC, merchantPublicId, publicKey, keyVersion)
```

2. Выполните запрос на проведения платежа (см. [документацию по API](https://developers.cloudpayments.ru/#oplata-po-kriptogramme)).

3. Если необходимо, покажите 3DS форму для подтверждения платежа

```
val acsUrl = transaction.acsUrl
val paReq = transaction.paReq
val md = transaction.transactionId
ThreeDsDialogFragment
	.newInstance(acsUrl, paReq, md)
	.show(supportFragmentManager, "3DS")
```

4. Для получения формы 3DS и получения результатов прохождения 3DS аутентификации реализуйте протокол ThreeDSDialogListener. Передайте в запрос также threeDsCallbackId, полученный в ответ на auth или charge

```
override fun onAuthorizationCompleted(md: String, paRes: String) {
	// Используйте md и paRes, для завершения оплаты
}

override fun onAuthorizationFailed(error: String?) {
	Log.d("Error", "AuthorizationFailed: $error")
}
```

5. Выполните запрос post3ds для завершения оплаты (см. [документацию по API](https://developers.cloudpayments.ru/#obrabotka-3-d-secure)).

### Другие функции

* Проверка карточного номера на корректность

```
Card.isValidNumber(cardNumber)

```

* Проверка срока действия карты

```
Card.isValidExpDate(expDate) // expDate в формате MM/yy

```

* Определение типа платежной системы

```
let cardType: CardType = Card.cardType(from: cardNumberString)
```

* Определение банка эмитента

```
val api = CloudpaymentsSDK.createApi(Constants.merchantPublicId)
api.getBinInfo(firstSixDigits)
	.subscribeOn(Schedulers.io())
	.observeOn(AndroidSchedulers.mainThread())
	.subscribe({ info -> Log.d("Bank name", info.bankName.orEmpty()) }, this::handleError)
```

* Шифрование карточных данных и создание криптограммы для отправки на сервер:
	1. Получите актуальный открытый ключ и версию ключа для шифрования используя метода API: https://api.cloudpayments.ru/payments/publickey
	2. Передайте данные о карте, ваш publicId, и полученные в шаге 1 данные в футнкцию для создания криптограммы:

```
val cryptogram = Card.createHexPacketFromData(
				cardNumber,
				cardExp,
				cardCvv,
				Constants.MERCHANT_PUBLIC_ID,
				PUBLIC_KEY,
				KEY_VERSION
			)
```

* Шифрование cvv при оплате сохраненной картой и создание криптограммы для отправки на сервер

```
val cvvCryptogramPacket = Card.cardCryptogramForCVV(cvv)
```

* Отображение 3DS формы и получении результата 3DS аутентификации

```
val acsUrl = transaction.acsUrl
val paReq = transaction.paReq
val md = transaction.transactionId
ThreeDsDialogFragment
	.newInstance(acsUrl, paReq, md)
	.show(supportFragmentManager, "3DS")

interface ThreeDSDialogListener {
	fun onAuthorizationCompleted(md: String, paRes: String)
	fun onAuthorizationFailed(error: String?)
}
```

### История обновлений:

### 2.1.1
* Изменен формат передачи чека, из-за частых изменений теперь он передается как Map\<String, Any\> (см. документацию)
* Исправлены ошибки

### 2.1.0
* Добавлена возможность использовать SDK в режиме одного способа оплаты (см. документацию)
* Учтена возможность работы на устройстве нескольких приложений с нашим SDK, теперь необходимо в build.gradle уровня приложения добавлять запись в Manifest Placeholders, для корректного возврата из приложения банка (см. документацию)
* Повышена стабильность работы

### 2.0.0
* Новый дизайн, новые возможности, добавлены новые способы оплаты, добавлена возможность изменять порядок отображения способов оплаты

#### 1.7.0
* Переход на новое API

#### 1.6.0
* Исправлена проблема с проверкой статуса транзакции, при использовании альтернативных способов оплаты на последнее версии Android

#### 1.5.16
* Добавлены обхъекты для удобного создания чека (PaymentDataReceipt) и подписки (PaymentDataRecurrent)

#### 1.5.12
* Убран параметр ipAddress из запросов

#### 1.5.11
* Добавлено уведомление плательщика о сохранении карты

#### 1.5.10
* Добавлен новый параметр в конфигурации: showResultScreenForSinglePaymentMode - Показывать или нет экран с результатом оплаты (по умолчанию true и экран будет отображен, используйте false чтобы SDK не показывало экран, и сразу вернула результат оплаты в ваше приложение), используется только в режимах отдельной кнопки.

#### 1.5.6
* Добавлен новый способ оплаты МИРPay


#### 1.5.5
* Добавлен новый способ оплаты SberPay

* Добавлен режим запуска SDK SberPay

#### 1.5.4
* Добавлен запуск SDK в режиме СБП

* Добавлен поиск по списку банков при оплате по СБП

* Добавлена расшифрока некоторых причин отказа в проведении платежа

* Введена проверка срока действия карты в зависимости от настроек шлюза

* Повышена надежность и стабильность работы

#### 1.5.2
* Теперь включать и выключать GPay можно в личном кабинете, больше нет необходимости делать это при конфигурации SDK и создавать после этого новые версии приложения

* Больше нет необходимости прописывать в конфигурации deeplink для возврата из приложений банков, SDK формирует свои deeplink

* Исправлены проблемы с некоторыми клавиатурами

* Оптимизирована валидация

* Минимально поддерживаемая версия Android API 23


#### 1.5.1
* Добавлен режим запуска SDK TinkoffPay

* Добавлена возможность педедать deeplink для перехода из приложения Tinkoff после оплаты

* Отключен YandexPay

#### 1.5.0
* Повышена надежность

#### 1.4.1
* Оптимизированны запросы

* Обновлены библиотеки

* ВНИМАНИЕ: Обновлен Yandex pay теперь для тестирования Yandex pay необоходимо в конфигурации SDK указывать testMode = true

#### 1.4.0
* Добавлен новый способ оплаты: оплата через СБП (см. документацию для получения более подробной информации: https://gitpub.cloudpayments.ru/integrations/sdk/cloudpayments-android/-/blob/master/README.md)

* Оптимизировано получение параметров шлюза и проверка доступности способов оплаты: теперь экран способов оплаты появляется сразу со всеми подключенными и доступными способами оплаты

* Внесено значительное количество небольших исправлений и улучшений


### Поддержка

По возникающим вопросам техничечкого характера обращайтесь на support@cp.ru

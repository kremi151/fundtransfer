package lu.mkremer.fundstransfer

import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.dto.MonetaryAmountDTO
import lu.mkremer.fundstransfer.datamodel.request.AccountBalanceRequest
import lu.mkremer.fundstransfer.datamodel.request.CreateAccountRequest
import lu.mkremer.fundstransfer.datamodel.request.MoneyTransferRequest
import lu.mkremer.fundstransfer.exception.UnsupportedCurrencyException
import lu.mkremer.fundstransfer.service.CurrencyExchanger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.math.RoundingMode


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionTests {

	companion object {
		// To not rely on an external service during integration tests, the exchange rates
		// used in these tests are mocked using the following values:
		private const val EUR_TO_JPY = 160.5
		private const val EUR_TO_CHF = 0.96
	}

	@LocalServerPort
	private var localPort: Int = 0

	@MockBean
	private lateinit var currencyExchanger: CurrencyExchanger

	private lateinit var restTemplate: TestRestTemplate

	@BeforeEach
	fun before() {
		restTemplate = TestRestTemplate(
			RestTemplateBuilder()
				.rootUri("http://localhost:${localPort}")
		)
	}

	@Test
	fun testTransferMoneyWithSameCurrenciesEverywhere() {
		`when`(currencyExchanger.supportsCurrency("EUR")).thenReturn(true)
		`when`(currencyExchanger.convert(
			any(),
			eq("EUR")
		)).thenAnswer {
			val monetaryAmount = it.getArgument<MonetaryAmountDTO>(0)
			when (monetaryAmount.currency) {
				"EUR" -> monetaryAmount.amount
				else -> throw UnsupportedCurrencyException(monetaryAmount.currency)
			}
		}

		testMoneyTransfer(
			sourceAccountCurrency = "EUR",
			targetAccountCurrency = "EUR",
			initialDeposit = MonetaryAmountDTO(100.0, "EUR"),
			transfer = MonetaryAmountDTO(25.0, "EUR"),
			expectedInitialBalanceAfterDeposit = BigDecimal("100.00"),
			expectedSourceBalanceAfterTransfer = BigDecimal("75.00"),
			expectedTargetBalanceAfterTransfer = BigDecimal("25.00"),
		)
	}

	@Test
	fun testTransferMoneyWithSameCurrenciesInBothAccounts() {
		`when`(currencyExchanger.supportsCurrency("JPY")).thenReturn(true)
		`when`(currencyExchanger.supportsCurrency("EUR")).thenReturn(true)
		`when`(currencyExchanger.convert(
			any(),
			eq("JPY")
		)).thenAnswer {
			val monetaryAmount = it.getArgument<MonetaryAmountDTO>(0)
			when (monetaryAmount.currency) {
				"JPY" -> monetaryAmount.amount
				"EUR" -> monetaryAmount.amount.multiply(EUR_TO_JPY.toBigDecimal())
				else -> throw UnsupportedCurrencyException(monetaryAmount.currency)
			}
		}
		`when`(currencyExchanger.convert(
			any(),
			eq("EUR")
		)).thenAnswer {
			val monetaryAmount = it.getArgument<MonetaryAmountDTO>(0)
			when (monetaryAmount.currency) {
				"JPY" -> monetaryAmount.amount.divide(EUR_TO_JPY.toBigDecimal(), 4, RoundingMode.HALF_UP)
				"EUR" -> monetaryAmount.amount
				else -> throw UnsupportedCurrencyException(monetaryAmount.currency)
			}
		}

		testMoneyTransfer(
			sourceAccountCurrency = "JPY",
			targetAccountCurrency = "JPY",
			initialDeposit = MonetaryAmountDTO(100.0, "EUR"),
			transfer = MonetaryAmountDTO(50.0, "EUR"),
			expectedInitialBalanceAfterDeposit = BigDecimal("16050.00"),
			expectedSourceBalanceAfterTransfer = BigDecimal("8025.00"),
			expectedTargetBalanceAfterTransfer = BigDecimal("8025.00"),
		)
	}

	@Test
	fun testTransferMoneyWithDifferentCurrenciesInBothAccounts() {
		`when`(currencyExchanger.supportsCurrency("JPY")).thenReturn(true)
		`when`(currencyExchanger.supportsCurrency("CHF")).thenReturn(true)
		`when`(currencyExchanger.supportsCurrency("EUR")).thenReturn(true)
		`when`(currencyExchanger.convert(
			any(),
			eq("JPY")
		)).thenAnswer {
			val monetaryAmount = it.getArgument<MonetaryAmountDTO>(0)
			when (monetaryAmount.currency) {
				"JPY" -> monetaryAmount.amount
				"EUR" -> monetaryAmount.amount.multiply(EUR_TO_JPY.toBigDecimal())
				"CHF" -> monetaryAmount.amount.divide(EUR_TO_CHF.toBigDecimal(), 4, RoundingMode.HALF_UP).multiply(EUR_TO_JPY.toBigDecimal())
				else -> throw UnsupportedCurrencyException(monetaryAmount.currency)
			}
		}
		`when`(currencyExchanger.convert(
			any(),
			eq("CHF")
		)).thenAnswer {
			val monetaryAmount = it.getArgument<MonetaryAmountDTO>(0)
			when (monetaryAmount.currency) {
				"JPY" -> monetaryAmount.amount.divide(EUR_TO_JPY.toBigDecimal(), 4, RoundingMode.HALF_UP).multiply(EUR_TO_CHF.toBigDecimal())
				"CHF" -> monetaryAmount.amount
				"EUR" -> monetaryAmount.amount.multiply(EUR_TO_CHF.toBigDecimal())
				else -> throw UnsupportedCurrencyException(monetaryAmount.currency)
			}
		}

		testMoneyTransfer(
			sourceAccountCurrency = "CHF",
			targetAccountCurrency = "JPY",
			initialDeposit = MonetaryAmountDTO(100.0, "EUR"),
			transfer = MonetaryAmountDTO(75.0, "EUR"),
			expectedInitialBalanceAfterDeposit = BigDecimal("96.00"),
			expectedSourceBalanceAfterTransfer = BigDecimal("24.00"),
			expectedTargetBalanceAfterTransfer = BigDecimal("12037.50"),
		)
	}

	private fun testMoneyTransfer(
		sourceAccountCurrency: String,
		targetAccountCurrency: String,
		initialDeposit: MonetaryAmountDTO,
		transfer: MonetaryAmountDTO,
		expectedInitialBalanceAfterDeposit: BigDecimal,
		expectedSourceBalanceAfterTransfer: BigDecimal,
		expectedTargetBalanceAfterTransfer: BigDecimal,
	) {
		val sourceAccount = createAccount(sourceAccountCurrency)
		val targetAccount = createAccount(targetAccountCurrency)

		var updatedSourceAccount = depositMoney(sourceAccount.id, initialDeposit.amount, initialDeposit.currency)
		assertEquals(expectedInitialBalanceAfterDeposit, updatedSourceAccount.balance, "Balance was updated correctly")
		assertEquals(sourceAccount.currency, updatedSourceAccount.currency, "Currency of source account must not change")

		var updatedTargetAccount = getAccount(targetAccount.id)
		assertEquals(BigDecimal("0.00"), updatedTargetAccount.balance, "Balance of target account must still be 0")
		assertEquals(targetAccount.currency, updatedTargetAccount.currency, "Currency of target account must not change")

		transferMoney(
			sourceAccountId = sourceAccount.id,
			targetAccountId = targetAccount.id,
			amount = transfer.amount,
			currency = transfer.currency,
		)

		updatedSourceAccount = getAccount(sourceAccount.id)
		assertEquals(expectedSourceBalanceAfterTransfer, updatedSourceAccount.balance, "Balance was updated correctly")
		assertEquals(sourceAccount.currency, updatedSourceAccount.currency, "Currency of source account must not change")

		updatedTargetAccount = getAccount(targetAccount.id)
		assertEquals(expectedTargetBalanceAfterTransfer, updatedTargetAccount.balance, "Balance was updated correctly")
		assertEquals(targetAccount.currency, updatedTargetAccount.currency, "Currency of source account must not change")
	}

	private fun createAccount(currency: String): AccountDTO {
		val request = CreateAccountRequest(currency = currency)
		val response = restTemplate.postForEntity("/account", request, AccountDTO::class.java)
		assertEquals(HttpStatus.CREATED, response.statusCode, "Status code must be 201 Created")

		val account = response.body
		assertNotNull(account, "Response body must not be null")

		// Tell the Kotlin compiler that we are sure that account cannot be null here
		account!!

		assertTrue(account.id.matches("^[0-9]{9}$".toRegex()), "The account ID must be 9 digits long")
		assertEquals(currency, account.currency, "The currency must be the one that was set in the request")
		assertEquals(BigDecimal(0.0), account.balance, "The starting balance must be 0")

		return account
	}

	private fun getAccount(accountId: String): AccountDTO {
		val response = restTemplate.getForEntity("/account/${accountId}", AccountDTO::class.java)
		assertEquals(HttpStatus.OK, response.statusCode, "Status code must be 200 Ok")

		val account = response.body
		assertNotNull(account, "Response body must not be null")

		return account!!
	}

	private fun depositMoney(accountId: String, amount: BigDecimal, currency: String): AccountDTO {
		val request = AccountBalanceRequest(
			accountId = accountId,
			amount = amount,
			currency = currency,
		)
		val response = restTemplate.postForEntity("/transaction/deposit", request, AccountDTO::class.java)
		assertEquals(HttpStatus.OK, response.statusCode, "Status code must be 200 Ok")

		val account = response.body
		assertNotNull(account, "Response body must not be null")

		// Tell the Kotlin compiler that we are sure that account cannot be null here
		account!!

		assertEquals(accountId, account.id)
		return account
	}

	private fun transferMoney(sourceAccountId: String, targetAccountId: String, amount: BigDecimal, currency: String): AccountDTO {
		val request = MoneyTransferRequest(
			sourceAccountId = sourceAccountId,
			targetAccountId = targetAccountId,
			amount = amount,
			currency = currency,
		)
		val response = restTemplate.postForEntity("/transaction/transfer", request, AccountDTO::class.java)
		assertEquals(HttpStatus.OK, response.statusCode, "Status code must be 200 Ok")

		val account = response.body
		assertNotNull(account, "Response body must not be null")

		// Tell the Kotlin compiler that we are sure that account cannot be null here
		account!!

		assertEquals(sourceAccountId, account.id)
		return account
	}

}

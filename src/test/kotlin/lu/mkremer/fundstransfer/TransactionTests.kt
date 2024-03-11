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

/**
 * Integration tests that cover money transfer between two accounts, in
 * different configurations.
 *
 * The tests that cover happy paths also implicitly cover the endpoints
 * for creating accounts and depositing money on them.
 */
class TransactionTests: AbstractIntegrationTest() {

	companion object {
		// To not rely on an external service during integration tests, the exchange rates
		// used in these tests are mocked using the following values:
		private const val EUR_TO_JPY = 160.5
		private const val EUR_TO_CHF = 0.96
	}

	@MockBean
	private lateinit var currencyExchanger: CurrencyExchanger

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
			debitAccountCurrency = "EUR",
			creditAccountCurrency = "EUR",
			initialDeposit = MonetaryAmountDTO(100.0, "EUR"),
			transfer = MonetaryAmountDTO(25.0, "EUR"),
			expectedInitialBalanceAfterDeposit = BigDecimal("100.00"),
			expectedDebitAccountBalanceAfterTransfer = BigDecimal("75.00"),
			expectedCreditAccountBalanceAfterTransfer = BigDecimal("25.00"),
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
			debitAccountCurrency = "JPY",
			creditAccountCurrency = "JPY",
			initialDeposit = MonetaryAmountDTO(100.0, "EUR"),
			transfer = MonetaryAmountDTO(50.0, "EUR"),
			expectedInitialBalanceAfterDeposit = BigDecimal("16050.00"),
			expectedDebitAccountBalanceAfterTransfer = BigDecimal("8025.00"),
			expectedCreditAccountBalanceAfterTransfer = BigDecimal("8025.00"),
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
			debitAccountCurrency = "CHF",
			creditAccountCurrency = "JPY",
			initialDeposit = MonetaryAmountDTO(100.0, "EUR"),
			transfer = MonetaryAmountDTO(75.0, "EUR"),
			expectedInitialBalanceAfterDeposit = BigDecimal("96.00"),
			expectedDebitAccountBalanceAfterTransfer = BigDecimal("24.00"),
			expectedCreditAccountBalanceAfterTransfer = BigDecimal("12037.50"),
		)
	}

	@Test
	fun testDepositAndWithdrawMoney() {
		`when`(currencyExchanger.supportsCurrency("JPY")).thenReturn(true);
		`when`(currencyExchanger.convert(any(), eq("JPY")))
			.thenAnswer {
				val monetaryAmount = it.getArgument<MonetaryAmountDTO>(0)
				if (monetaryAmount.currency == "JPY") {
					monetaryAmount.amount
				} else throw UnsupportedCurrencyException(monetaryAmount.currency)
			}

		val account = createAccount("JPY")

		var updatedAccount = depositMoney(account.id, 1000.8.toBigDecimal(), "JPY")
		assertEquals(updatedAccount.balance, BigDecimal("1000.80"), "The right amount of money was deposited")
		assertEquals(updatedAccount.currency, "JPY", "The currency must not change")
		assertEquals(updatedAccount.id, account.id, "The returned account id must not change")

		// Also check if getting the accounts returns the expected amount of money
		updatedAccount = getAccount(account.id)
		assertEquals(updatedAccount.balance, BigDecimal("1000.80"), "The right amount of money was deposited")
		assertEquals(updatedAccount.currency, "JPY", "The currency must not change")
		assertEquals(updatedAccount.id, account.id, "The returned account id must not change")

		updatedAccount = withdrawMoney(account.id, 180.6.toBigDecimal(), "JPY")
		assertEquals(updatedAccount.balance, BigDecimal("820.20"), "The right amount of money was withdrawn")
		assertEquals(updatedAccount.currency, "JPY", "The currency must not change")
		assertEquals(updatedAccount.id, account.id, "The returned account id must not change")

		// Also check if getting the accounts returns the expected amount of money
		updatedAccount = getAccount(account.id)
		assertEquals(updatedAccount.balance, BigDecimal("820.20"), "The right amount of money was deposited")
		assertEquals(updatedAccount.currency, "JPY", "The currency must not change")
		assertEquals(updatedAccount.id, account.id, "The returned account id must not change")
	}

	private fun testMoneyTransfer(
		debitAccountCurrency: String,
		creditAccountCurrency: String,
		initialDeposit: MonetaryAmountDTO,
		transfer: MonetaryAmountDTO,
		expectedInitialBalanceAfterDeposit: BigDecimal,
		expectedDebitAccountBalanceAfterTransfer: BigDecimal,
		expectedCreditAccountBalanceAfterTransfer: BigDecimal,
	) {
		val debitAccount = createAccount(debitAccountCurrency)
		val creditAccount = createAccount(creditAccountCurrency)

		var updatedDebitAccount = depositMoney(debitAccount.id, initialDeposit.amount, initialDeposit.currency)
		assertEquals(expectedInitialBalanceAfterDeposit, updatedDebitAccount.balance, "Balance was updated correctly")
		assertEquals(debitAccount.currency, updatedDebitAccount.currency, "Currency of debit account must not change")

		var updatedCreditAccount = getAccount(creditAccount.id)
		assertEquals(BigDecimal("0.00"), updatedCreditAccount.balance, "Balance of credit account must still be 0")
		assertEquals(creditAccount.currency, updatedCreditAccount.currency, "Currency of credit account must not change")

		transferMoney(
			debitAccountId = debitAccount.id,
			creditAccountId = creditAccount.id,
			amount = transfer.amount,
			currency = transfer.currency,
		)

		updatedDebitAccount = getAccount(debitAccount.id)
		assertEquals(expectedDebitAccountBalanceAfterTransfer, updatedDebitAccount.balance, "Balance was updated correctly")
		assertEquals(debitAccount.currency, updatedDebitAccount.currency, "Currency of debit account must not change")

		updatedCreditAccount = getAccount(creditAccount.id)
		assertEquals(expectedCreditAccountBalanceAfterTransfer, updatedCreditAccount.balance, "Balance was updated correctly")
		assertEquals(creditAccount.currency, updatedCreditAccount.currency, "Currency of credit account must not change")
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

	private fun performBalanceRequest(endpoint: String, accountId: String, amount: BigDecimal, currency: String): AccountDTO {
		val request = AccountBalanceRequest(
			accountId = accountId,
			amount = amount,
			currency = currency,
		)
		val response = restTemplate.postForEntity("/transaction/$endpoint", request, AccountDTO::class.java)
		assertEquals(HttpStatus.OK, response.statusCode, "Status code must be 200 Ok")

		val account = response.body
		assertNotNull(account, "Response body must not be null")

		// Tell the Kotlin compiler that we are sure that account cannot be null here
		account!!

		assertEquals(accountId, account.id)
		return account
	}

	private fun depositMoney(accountId: String, amount: BigDecimal, currency: String): AccountDTO {
		return performBalanceRequest("deposit", accountId, amount, currency)
	}

	private fun withdrawMoney(accountId: String, amount: BigDecimal, currency: String): AccountDTO {
		return performBalanceRequest("withdraw", accountId, amount, currency)
	}

	private fun transferMoney(debitAccountId: String, creditAccountId: String, amount: BigDecimal, currency: String): AccountDTO {
		val request = MoneyTransferRequest(
			debitAccountId = debitAccountId,
			creditAccountId = creditAccountId,
			amount = amount,
			currency = currency,
		)
		val response = restTemplate.postForEntity("/transaction/transfer", request, AccountDTO::class.java)
		assertEquals(HttpStatus.OK, response.statusCode, "Status code must be 200 Ok")

		val account = response.body
		assertNotNull(account, "Response body must not be null")

		// Tell the Kotlin compiler that we are sure that account cannot be null here
		account!!

		assertEquals(debitAccountId, account.id)
		return account
	}

}

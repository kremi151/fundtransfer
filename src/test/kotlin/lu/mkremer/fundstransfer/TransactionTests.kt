package lu.mkremer.fundstransfer

import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.dto.MonetaryAmountDTO
import lu.mkremer.fundstransfer.datamodel.request.AccountBalanceRequest
import lu.mkremer.fundstransfer.datamodel.request.CreateAccountRequest
import lu.mkremer.fundstransfer.datamodel.request.MoneyTransferRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import java.math.BigDecimal

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

	@Test
	fun testTransferMoneyWithSameCurrenciesEverywhere() {
		mockExchangeRates(mapOf("EUR" to 1.0))

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
		mockExchangeRates(mapOf(
			"EUR" to 1.0,
			"JPY" to EUR_TO_JPY,
		))

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
		mockExchangeRates(mapOf(
			"EUR" to 1.0,
			"JPY" to EUR_TO_JPY,
			"CHF" to EUR_TO_CHF,
		))

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
		mockExchangeRates(mapOf("JPY" to EUR_TO_JPY))

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

	@Test
	fun testDepositAndWithdrawMoneyWithInsufficientMoney() {
		mockExchangeRates(mapOf("JPY" to EUR_TO_JPY))

		val account = createAccount("JPY")

		var updatedAccount = depositMoney(account.id, 1000.0.toBigDecimal(), "JPY")
		assertEquals(updatedAccount.balance, BigDecimal("1000.00"), "The right amount of money was deposited")
		assertEquals(updatedAccount.currency, "JPY", "The currency must not change")
		assertEquals(updatedAccount.id, account.id, "The returned account id must not change")

		// Also check if getting the accounts returns the expected amount of money
		updatedAccount = getAccount(account.id)
		assertEquals(updatedAccount.balance, BigDecimal("1000.00"), "The right amount of money was deposited")
		assertEquals(updatedAccount.currency, "JPY", "The currency must not change")
		assertEquals(updatedAccount.id, account.id, "The returned account id must not change")

		val validationError = attemptToWithdrawMoney(account.id, 1000.1.toBigDecimal(), "JPY")

		assertEquals("Account ${account.id} has insufficient balance: 0.10 JPY are missing", validationError.message)
		assertNull(validationError.fieldErrors)

		// Check if getting the accounts returns the unchanged amount of money
		updatedAccount = getAccount(account.id)
		assertEquals(updatedAccount.balance, BigDecimal("1000.00"), "The right amount of money was deposited")
		assertEquals(updatedAccount.currency, "JPY", "The currency must not change")
		assertEquals(updatedAccount.id, account.id, "The returned account id must not change")
	}

	@Test
	fun testTransferMoneyWithInsufficientMoneyInDebitAccount() {
		mockExchangeRates(mapOf("JPY" to EUR_TO_JPY))

		val debitAccount = createAccount("JPY")
		val creditAccount = createAccount("JPY")

		var updatedDebitAccount = depositMoney(debitAccount.id, 2000.0.toBigDecimal(), "JPY")
		assertEquals(BigDecimal("2000.00"), updatedDebitAccount.balance, "Balance was updated correctly")
		assertEquals(debitAccount.currency, updatedDebitAccount.currency, "Currency of debit account must not change")

		var updatedCreditAccount = getAccount(creditAccount.id)
		assertEquals(BigDecimal("0.00"), updatedCreditAccount.balance, "Balance of credit account must still be 0")
		assertEquals(creditAccount.currency, updatedCreditAccount.currency, "Currency of credit account must not change")

		val validationError = attemptToTransferMoney(
			debitAccountId = debitAccount.id,
			creditAccountId = creditAccount.id,
			amount = 2000.1.toBigDecimal(),
			currency = "JPY",
		)

		assertEquals("Account ${debitAccount.id} has insufficient balance: 0.10 JPY are missing", validationError.message)
		assertNull(validationError.fieldErrors)

		updatedDebitAccount = getAccount(debitAccount.id)
		assertEquals(BigDecimal("2000.00"), updatedDebitAccount.balance, "Balance was not updated")
		assertEquals(debitAccount.currency, updatedDebitAccount.currency, "Currency of debit account must not change")

		updatedCreditAccount = getAccount(creditAccount.id)
		assertEquals(BigDecimal("0.00"), updatedCreditAccount.balance, "Balance was not updated")
		assertEquals(creditAccount.currency, updatedCreditAccount.currency, "Currency of credit account must not change")
	}

	@Test
	fun testTransferringMoneyToTheSameAccount() {
		mockExchangeRates(mapOf("CHF" to EUR_TO_CHF))

		val account = createAccount("CHF")

		val response = attemptToTransferMoney(
			debitAccountId = account.id,
			creditAccountId = account.id,
			amount = BigDecimal.TEN,
			currency = "CHF",
		)

		assertEquals(
			"Transfer could not be performed: Transferring money from and to the same account is not allowed",
			response.message,
		)
		assertNull(response.fieldErrors)
	}

	@Test
	fun testDepositMoneyWithMissingExchangeRates() {
		mockFailedExchangeRatesFetch()

		val account = createAccount("EUR")

		postRequestExpectingStatus(
			path = "/transaction/deposit",
			requestBody = AccountBalanceRequest(
				accountId = account.id,
				amount = BigDecimal.TEN,
				currency = "EUR",
			),
			expectedStatus = HttpStatus.SERVICE_UNAVAILABLE,
		)
	}

	@Test
	fun testDepositMoneyOnUnknownAccount() {
		mockExchangeRates(mapOf("EUR" to 1.0))

		postRequestExpectingStatus(
			path = "/transaction/deposit",
			requestBody = AccountBalanceRequest(
				accountId = "000000000", // This is a reserved ID which will never be assigned to any account
				amount = BigDecimal.TEN,
				currency = "EUR",
			),
			expectedStatus = HttpStatus.NOT_FOUND,
		)
	}

	@Test
	fun testWithdrawMoneyWithMissingExchangeRates() {
		mockExchangeRates(mapOf("EUR" to 1.0))

		val account = createAccount("EUR")

		depositMoney(
			accountId = account.id,
			amount = BigDecimal.TEN,
			currency = "EUR",
		)

		mockFailedExchangeRatesFetch()

		postRequestExpectingStatus(
			path = "/transaction/withdraw",
			requestBody = AccountBalanceRequest(
				accountId = account.id,
				amount = BigDecimal.ONE,
				currency = "EUR",
			),
			expectedStatus = HttpStatus.SERVICE_UNAVAILABLE,
		)
	}

	@Test
	fun testWithdrawMoneyFromUnknownAccount() {
		mockExchangeRates(mapOf("EUR" to 1.0))

		postRequestExpectingStatus(
			path = "/transaction/withdraw",
			requestBody = AccountBalanceRequest(
				accountId = "000000000", // This is a reserved ID which will never be assigned to any account
				amount = BigDecimal.ONE,
				currency = "EUR",
			),
			expectedStatus = HttpStatus.NOT_FOUND,
		)
	}

	@Test
	fun testTransferMoneyWithMissingExchangeRates() {
		mockExchangeRates(mapOf("EUR" to 1.0))

		val debitAccount = createAccount("EUR")
		val creditAccount = createAccount("EUR")

		depositMoney(
			accountId = debitAccount.id,
			amount = BigDecimal.TEN,
			currency = "EUR",
		)

		mockFailedExchangeRatesFetch()

		postRequestExpectingStatus(
			path = "/transaction/transfer",
			requestBody = MoneyTransferRequest(
				debitAccountId = debitAccount.id,
				creditAccountId = creditAccount.id,
				amount = BigDecimal.ONE,
				currency = "EUR",
			),
			expectedStatus = HttpStatus.SERVICE_UNAVAILABLE,
		)
	}

	@Test
	fun testTransferMoneyFromUnknownDebitAccount() {
		mockExchangeRates(mapOf("EUR" to 1.0))

		val creditAccount = createAccount("EUR")

		postRequestExpectingStatus(
			path = "/transaction/transfer",
			requestBody = MoneyTransferRequest(
				debitAccountId = "000000000", // This is a reserved ID which will never be assigned to any account
				creditAccountId = creditAccount.id,
				amount = BigDecimal.ONE,
				currency = "EUR",
			),
			expectedStatus = HttpStatus.NOT_FOUND,
		)
	}

	@Test
	fun testTransferMoneyFromUnknownCreditAccount() {
		mockExchangeRates(mapOf("EUR" to 1.0))

		val debitAccount = createAccount("EUR")

		depositMoney(
			accountId = debitAccount.id,
			amount = BigDecimal.TEN,
			currency = "EUR",
		)

		postRequestExpectingStatus(
			path = "/transaction/transfer",
			requestBody = MoneyTransferRequest(
				debitAccountId = debitAccount.id,
				creditAccountId = "000000000", // This is a reserved ID which will never be assigned to any account
				amount = BigDecimal.ONE,
				currency = "EUR",
			),
			expectedStatus = HttpStatus.NOT_FOUND,
		)
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

	private fun postRequestExpectingStatus(path: String, requestBody: Any, expectedStatus: HttpStatusCode) {
		val response = restTemplate.postForEntity(path, requestBody, Any::class.java)
		assertEquals(expectedStatus, response.statusCode, "Status code must be as expected")

		val body = response.body
		assertNull(body, "Response body must be null")
	}

}

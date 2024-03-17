package lu.mkremer.fundstransfer

import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.dto.MonetaryAmountDTO
import lu.mkremer.fundstransfer.datamodel.request.AccountBalanceRequest
import lu.mkremer.fundstransfer.datamodel.request.CreateAccountRequest
import lu.mkremer.fundstransfer.datamodel.request.MoneyTransferRequest
import lu.mkremer.fundstransfer.util.Assertions.assertComparableEquals
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import java.lang.invoke.MethodHandles
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Integration tests that cover money transfer between two accounts, in
 * different configurations.
 *
 * The tests that cover happy paths also implicitly cover the endpoints
 * for creating accounts and depositing money on them.
 */
class TransactionTests: AbstractIntegrationTest() {

	companion object {
		private val LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

		// To not rely on an external service during integration tests, the exchange rates
		// used in these tests are mocked using the following values:
		private val EUR_TO_EUR = "1.0".toBigDecimal()
		private val EUR_TO_JPY = "160.5".toBigDecimal()
		private val EUR_TO_CHF = "0.96".toBigDecimal()
	}

	/**
	 * Test for verifying whether a money transfer with the same currency used
	 * in both debit and credit account works.
	 */
	@Test
	fun testTransferMoneyWithSameCurrenciesEverywhere() {
		mockExchangeRates(mapOf("EUR" to EUR_TO_EUR))

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

	/**
	 * Test for verifying whether a money transfer with a different currency
	 * thant used in both debit and credit account works.
	 */
	@Test
	fun testTransferMoneyWithSameCurrenciesInBothAccounts() {
		mockExchangeRates(mapOf(
			"EUR" to EUR_TO_EUR,
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

	/**
	 * Test for verifying whether a money transfer between two accounts with
	 * different currencies works.
	 */
	@Test
	fun testTransferMoneyWithDifferentCurrenciesInBothAccounts() {
		mockExchangeRates(mapOf(
			"EUR" to EUR_TO_EUR,
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

	/**
	 * Test for verifying whether a money deposit and withdraw on/from an
	 * account works.
	 */
	@Test
	fun testDepositAndWithdrawMoney() {
		mockExchangeRates(mapOf("JPY" to EUR_TO_JPY))

		val account = createAccount("JPY")

		var updatedAccount = depositMoney(account.id, 1000.8.toBigDecimal(), "JPY")
		assertComparableEquals(BigDecimal("1000.80"), updatedAccount.balance, "The right amount of money was deposited")
		assertEquals("JPY", updatedAccount.currency, "The currency must not change")
		assertEquals(account.id, updatedAccount.id, "The returned account id must not change")

		// Also check if getting the accounts returns the expected amount of money
		updatedAccount = getAccount(account.id)
		assertComparableEquals(BigDecimal("1000.80"), updatedAccount.balance, "The right amount of money was deposited")
		assertEquals("JPY", updatedAccount.currency, "The currency must not change")
		assertEquals(account.id, updatedAccount.id, "The returned account id must not change")

		updatedAccount = withdrawMoney(account.id, 180.6.toBigDecimal(), "JPY")
		assertComparableEquals(BigDecimal("820.20"), updatedAccount.balance, "The right amount of money was withdrawn")
		assertEquals("JPY", updatedAccount.currency, "The currency must not change")
		assertEquals(account.id, updatedAccount.id, "The returned account id must not change")

		// Also check if getting the accounts returns the expected amount of money
		updatedAccount = getAccount(account.id)
		assertComparableEquals(BigDecimal("820.20"), updatedAccount.balance, "The right amount of money was deposited")
		assertEquals("JPY", updatedAccount.currency, "The currency must not change")
		assertEquals(account.id, updatedAccount.id, "The returned account id must not change")
	}

	/**
	 * Test for verifying whether a withdrawal from an account with insufficient
	 * balance results in an error.
	 */
	@Test
	fun testDepositAndWithdrawMoneyWithInsufficientMoney() {
		mockExchangeRates(mapOf("JPY" to EUR_TO_JPY))

		val account = createAccount("JPY")

		var updatedAccount = depositMoney(account.id, 1000.0.toBigDecimal(), "JPY")
		assertComparableEquals(BigDecimal("1000.00"), updatedAccount.balance, "The right amount of money was deposited")
		assertEquals("JPY", updatedAccount.currency, "The currency must not change")
		assertEquals(account.id, updatedAccount.id, "The returned account id must not change")

		// Also check if getting the accounts returns the expected amount of money
		updatedAccount = getAccount(account.id)
		assertComparableEquals(BigDecimal("1000.00"), updatedAccount.balance, "The right amount of money was deposited")
		assertEquals("JPY", updatedAccount.currency, "The currency must not change")
		assertEquals(account.id, updatedAccount.id, "The returned account id must not change")

		val validationError = attemptToWithdrawMoney(account.id, 1000.1.toBigDecimal(), "JPY")

		assertEquals("Account ${account.id} has insufficient balance: 0.10 JPY are missing", validationError.message)
		assertNull(validationError.fieldErrors)

		// Check if getting the accounts returns the unchanged amount of money
		updatedAccount = getAccount(account.id)
		assertComparableEquals(BigDecimal("1000.00"), updatedAccount.balance, "The right amount of money was deposited")
		assertEquals("JPY", updatedAccount.currency, "The currency must not change")
		assertEquals(account.id, updatedAccount.id, "The returned account id must not change")
	}

	/**
	 * Test for verifying whether a transfer from an account with insufficient
	 * balance results in an error.
	 */
	@Test
	fun testTransferMoneyWithInsufficientMoneyInDebitAccount() {
		mockExchangeRates(mapOf("JPY" to EUR_TO_JPY))

		val debitAccount = createAccount("JPY")
		val creditAccount = createAccount("JPY")

		var updatedDebitAccount = depositMoney(debitAccount.id, 2000.0.toBigDecimal(), "JPY")
		assertComparableEquals(BigDecimal("2000.00"), updatedDebitAccount.balance, "Balance was updated correctly")
		assertEquals(debitAccount.currency, updatedDebitAccount.currency, "Currency of debit account must not change")

		var updatedCreditAccount = getAccount(creditAccount.id)
		assertComparableEquals(BigDecimal("0.00"), updatedCreditAccount.balance, "Balance of credit account must still be 0")
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
		assertComparableEquals(BigDecimal("2000.00"), updatedDebitAccount.balance, "Balance was not updated")
		assertEquals(debitAccount.currency, updatedDebitAccount.currency, "Currency of debit account must not change")

		updatedCreditAccount = getAccount(creditAccount.id)
		assertComparableEquals(BigDecimal("0.00"), updatedCreditAccount.balance, "Balance was not updated")
		assertEquals(creditAccount.currency, updatedCreditAccount.currency, "Currency of credit account must not change")
	}

	/**
	 * Test for verifying whether a transfer from and to the same account
	 * results in an error.
	 */
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

	/**
	 * Test for verifying whether a deposit fails when the exchange rates could
	 * not be fetched.
	 */
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

	/**
	 * Test for verifying whether a deposit on an unknown account results in
	 * an error.
	 */
	@Test
	fun testDepositMoneyOnUnknownAccount() {
		mockExchangeRates(mapOf("EUR" to EUR_TO_EUR))

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

	/**
	 * Test for verifying whether a withdrawal fails when the exchange rates
	 * could not be fetched.
	 */
	@Test
	fun testWithdrawMoneyWithMissingExchangeRates() {
		mockExchangeRates(mapOf("EUR" to EUR_TO_EUR))

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

	/**
	 * Test for verifying whether a withdrawal from an unknown account results
	 * in an error.
	 */
	@Test
	fun testWithdrawMoneyFromUnknownAccount() {
		mockExchangeRates(mapOf("EUR" to EUR_TO_EUR))

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

	/**
	 * Test for verifying whether a transfer fails when the exchange rates could
	 * not be fetched.
	 */
	@Test
	fun testTransferMoneyWithMissingExchangeRates() {
		mockExchangeRates(mapOf("EUR" to EUR_TO_EUR))

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

	/**
	 * Test for verifying whether a transfer from an unknown account results in
	 * an error.
	 */
	@Test
	fun testTransferMoneyFromUnknownDebitAccount() {
		mockExchangeRates(mapOf("EUR" to EUR_TO_EUR))

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

	/**
	 * Test for verifying whether a transfer to an unknown account results in
	 * an error.
	 */
	@Test
	fun testTransferMoneyFromUnknownCreditAccount() {
		mockExchangeRates(mapOf("EUR" to EUR_TO_EUR))

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

	/**
	 * This test is testing the case where multiple money transfers are being
	 * performed on common accounts, concurrently.
	 * For this, 9 accounts are created, each having a start capital of
	 * 100 Euros, converted to their individual currencies.
	 * The supported currencies for this test are Euro, Japanese Yen and
	 * Swiss Francs, each having 3 accounts using that currency.
	 * Every account will have a dedicated thread performing money transfers
	 * to the other accounts (4 times per credit account).
	 * In the end, we verify that no transfer request failed, and whether the
	 * end balances of the accounts are as expected.
	 * Since no randomization is used, and the only uncertainty being the
	 * execution order of transfers due to multithreading, the expected end
	 * balances are deterministic.
	 */
	@Test
	fun testConcurrentMoneyTransfer() {
		val exchangeRates = mapOf(
			"EUR" to EUR_TO_EUR,
			"JPY" to EUR_TO_JPY,
			"CHF" to EUR_TO_CHF,
		)
		mockExchangeRates(exchangeRates)

		val numAccounts = exchangeRates.size * exchangeRates.size

		val supportedCurrencies = exchangeRates.keys.toList()

		// Prepare accounts
		val accounts = (0 until numAccounts).map {
			createAccount(supportedCurrencies[it % supportedCurrencies.size]).let { account ->
				// Start capital of 100 Euro
				val updatedAccount = depositMoney(account.id, 100.0.toBigDecimal(), "EUR")
				assertEquals(account.id, updatedAccount.id)
				assertEquals(account.currency, updatedAccount.currency)
				assertComparableEquals(exchangeRates[account.currency]!!.multiply(100.0.toBigDecimal()), updatedAccount.balance)

				updatedAccount
			}
		}

		// Verify whether every account has the expected start capital
		val expectedStartCapitalPerBalance = mapOf(
			"EUR" to 100.0.toBigDecimal(),
			"CHF" to 96.0.toBigDecimal(),
			"JPY" to 16050.0.toBigDecimal(),
		)
		accounts.forEach {
			val expectedStartCapital = expectedStartCapitalPerBalance[it.currency]
			assertNotNull(expectedStartCapital, "Expected start capital must not be null")
			assertComparableEquals(expectedStartCapital!!, it.balance, "Account must have the expected start capital")
		}

		val executor = Executors.newFixedThreadPool(numAccounts)
		val countDownLatch = CountDownLatch(numAccounts)
		val failedTransfers = AtomicInteger(0)

		try {
			// Spawn one thread per account, which transfer money of a value of
			// 2 (in the currency of the debit account) to the other accounts,
			// in total 4 times.
			for (i in 0 until numAccounts) {
				executor.submit {
					val debitAccount = accounts[i]
					// Build the sequence of credit accounts to be used in this
					// thread, by repeating and account different to the debit
					// one 4 times in a zigzag fashion.
					val creditAccounts = accounts
						.filterIndexed { index, _ -> index != i }
						.let {
							it + it.reversed() + it + it.reversed()
						}
					try {
						creditAccounts.forEach { creditAccount ->
							try {
								// Transfer an amount of 2 in the debit account's currency
								transferMoney(
									debitAccountId = debitAccount.id,
									creditAccountId = creditAccount.id,
									amount = 2.0.toBigDecimal(),
									debitAccount.currency,
								)
							} catch (e: Exception) {
								LOGGER.error("Concurrent transfer failed", e)
								failedTransfers.incrementAndGet()
							}
						}
					} finally {
					    countDownLatch.countDown()
					}
				}
			}
		} finally {
			countDownLatch.await()
		    executor.shutdown()
			executor.awaitTermination(1, TimeUnit.SECONDS)
		}

		// Retrieve updated account information
		val updatedAccounts = accounts.map {
			getAccount(it.id)
		}

		assertEquals(0, failedTransfers.get(), "No transfer should have failed")

		// Verify whether the accounts have the expected balance after the concurrent transfers
		val expectedBalancePerCurrency = mapOf(
			"EUR" to 77.08.toBigDecimal(),
			"CHF" to 71.16.toBigDecimal(),
			"JPY" to 23866.44.toBigDecimal(),
		)
		updatedAccounts.forEach {
			val expectedBalance = expectedBalancePerCurrency[it.currency]
			assertNotNull(expectedBalance, "Expected balance must not be null")
			assertComparableEquals(expectedBalance!!, it.balance, "Account must have the expected balance")
		}
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
		assertComparableEquals(expectedInitialBalanceAfterDeposit, updatedDebitAccount.balance)
		assertEquals(debitAccount.currency, updatedDebitAccount.currency, "Currency of debit account must not change")

		var updatedCreditAccount = getAccount(creditAccount.id)
		assertComparableEquals(BigDecimal.ZERO, updatedCreditAccount.balance, "Balance of credit account must still be 0")
		assertEquals(creditAccount.currency, updatedCreditAccount.currency, "Currency of credit account must not change")

		transferMoney(
			debitAccountId = debitAccount.id,
			creditAccountId = creditAccount.id,
			amount = transfer.amount,
			currency = transfer.currency,
		)

		updatedDebitAccount = getAccount(debitAccount.id)
		assertComparableEquals(expectedDebitAccountBalanceAfterTransfer, updatedDebitAccount.balance, "Balance was updated correctly")
		assertEquals(debitAccount.currency, updatedDebitAccount.currency, "Currency of debit account must not change")

		updatedCreditAccount = getAccount(creditAccount.id)
		assertComparableEquals(expectedCreditAccountBalanceAfterTransfer, updatedCreditAccount.balance, "Balance was updated correctly")
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
		assertComparableEquals(BigDecimal.ZERO, account.balance, "The starting balance must be 0")

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

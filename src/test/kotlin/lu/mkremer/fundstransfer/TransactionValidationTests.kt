package lu.mkremer.fundstransfer

import lu.mkremer.fundstransfer.datamodel.dto.ValidationErrorDTO
import lu.mkremer.fundstransfer.datamodel.jpa.Account
import lu.mkremer.fundstransfer.datamodel.request.AccountBalanceRequest
import lu.mkremer.fundstransfer.datamodel.request.MoneyTransferRequest
import lu.mkremer.fundstransfer.repository.AccountRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import java.math.BigDecimal

/**
 * Integration tests to validate user inputs for the transaction controller.
 * Successful validations are covered by [TransactionTests].
 */
class TransactionValidationTests: AbstractIntegrationTest() {

    companion object {
        private const val GENERAL_ERROR_MESSAGE = "At least one submitted property has an invalid value"

        private const val ERROR_MESSAGE_ACCOUNT_ID = "The account ID must be a 9-digit number (with leading zeros)"
        private const val ERROR_MESSAGE_CURRENCY = "The currency is not an uppercase char sequence of 3 letters, or not supported"
        private const val ERROR_MESSAGE_AMOUNT = "The amount must be strictly positive"

        private const val PROPERTY_ACCOUNT_ID = "accountId"
        private const val PROPERTY_DEBIT_ACCOUNT_ID = "debitAccountId"
        private const val PROPERTY_CREDIT_ACCOUNT_ID = "creditAccountId"
        private const val PROPERTY_CURRENCY = "currency"
        private const val PROPERTY_AMOUNT = "amount"
    }

    @MockBean
    private lateinit var accountRepository: AccountRepository

    @BeforeEach
    fun setupStaticExchangeRates() {
        // For the sake of these tests, we just specify some static exchange
        // rates to make the currency validation work
        mockExchangeRates(mapOf("EUR" to 1.0))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "1234", "1234567890", "12345678p"])
    fun testDepositMoneyWithInvalidAccountId(accountId: String) {
        val error = attemptToDepositMoney(
            accountId = accountId,
            currency = "EUR",
            amount = BigDecimal.ONE,
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(PROPERTY_ACCOUNT_ID to ERROR_MESSAGE_ACCOUNT_ID), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Euro", "eur", "EURO", "eu", "EU", "XYZ"])
    fun testDepositMoneyWithInvalidCurrency(currency: String) {
        val error = attemptToDepositMoney(
            accountId = "123456789",
            currency = currency,
            amount = BigDecimal.ONE,
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(PROPERTY_CURRENCY to ERROR_MESSAGE_CURRENCY), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    @ParameterizedTest
    @ValueSource(doubles = [0.0, -1.0, -9999.9999])
    fun testDepositMoneyWithInvalidAmount(amount: Double) {
        val error = attemptToDepositMoney(
            accountId = "123456789",
            currency = "EUR",
            amount = amount.toBigDecimal(),
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(PROPERTY_AMOUNT to ERROR_MESSAGE_AMOUNT), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    @Test
    fun testDepositMoneyWithMultipleInvalidProperties() {
        val error = attemptToDepositMoney(
            accountId = "123",
            currency = "eur",
            amount = BigDecimal.ZERO,
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(
            PROPERTY_ACCOUNT_ID to ERROR_MESSAGE_ACCOUNT_ID,
            PROPERTY_CURRENCY to ERROR_MESSAGE_CURRENCY,
            PROPERTY_AMOUNT to ERROR_MESSAGE_AMOUNT,
        ), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "1234", "1234567890", "12345678p"])
    fun testWithdrawMoneyWithInvalidAccountId(accountId: String) {
        val error = attemptToWithdrawMoney(
            accountId = accountId,
            currency = "EUR",
            amount = BigDecimal.ONE,
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(PROPERTY_ACCOUNT_ID to ERROR_MESSAGE_ACCOUNT_ID), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Euro", "eur", "EURO", "eu", "EU", "XYZ"])
    fun testWithdrawMoneyWithInvalidCurrency(currency: String) {
        val error = attemptToWithdrawMoney(
            accountId = "123456789",
            currency = currency,
            amount = BigDecimal.ONE,
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(PROPERTY_CURRENCY to ERROR_MESSAGE_CURRENCY), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    @ParameterizedTest
    @ValueSource(doubles = [0.0, -1.0, -9999.9999])
    fun testWithdrawMoneyWithInvalidAmount(amount: Double) {
        val error = attemptToWithdrawMoney(
            accountId = "123456789",
            currency = "EUR",
            amount = amount.toBigDecimal(),
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(PROPERTY_AMOUNT to ERROR_MESSAGE_AMOUNT), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    @Test
    fun testWithdrawMoneyWithMultipleInvalidProperties() {
        val error = attemptToWithdrawMoney(
            accountId = "123",
            currency = "eur",
            amount = BigDecimal.ZERO,
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(
            PROPERTY_ACCOUNT_ID to ERROR_MESSAGE_ACCOUNT_ID,
            PROPERTY_CURRENCY to ERROR_MESSAGE_CURRENCY,
            PROPERTY_AMOUNT to ERROR_MESSAGE_AMOUNT,
        ), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "1234", "1234567890", "12345678p"])
    fun testTransferMoneyWithInvalidDebitAccountId(accountId: String) {
        val error = attemptToTransferMoney(
            debitAccountId = accountId,
            creditAccountId = "123456789",
            currency = "EUR",
            amount = BigDecimal.ONE,
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(PROPERTY_DEBIT_ACCOUNT_ID to ERROR_MESSAGE_ACCOUNT_ID), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "1234", "1234567890", "12345678p"])
    fun testTransferMoneyWithInvalidCreditAccountId(accountId: String) {
        val error = attemptToTransferMoney(
            debitAccountId = "123456789",
            creditAccountId = accountId,
            currency = "EUR",
            amount = BigDecimal.ONE,
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(PROPERTY_CREDIT_ACCOUNT_ID to ERROR_MESSAGE_ACCOUNT_ID), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Euro", "eur", "EURO", "eu", "EU", "XYZ"])
    fun testTransferMoneyWithInvalidCurrency(currency: String) {
        val error = attemptToTransferMoney(
            debitAccountId = "123456789",
            creditAccountId = "123456789",
            currency = currency,
            amount = BigDecimal.ONE,
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(PROPERTY_CURRENCY to ERROR_MESSAGE_CURRENCY), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    @ParameterizedTest
    @ValueSource(doubles = [0.0, -1.0, -9999.9999])
    fun testTransferMoneyWithInvalidAmount(amount: Double) {
        val error = attemptToTransferMoney(
            debitAccountId = "123456789",
            creditAccountId = "123456789",
            currency = "EUR",
            amount = amount.toBigDecimal(),
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(PROPERTY_AMOUNT to ERROR_MESSAGE_AMOUNT), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    @Test
    fun testTransferMoneyWithMultipleInvalidProperties() {
        val error = attemptToTransferMoney(
            debitAccountId = "123",
            creditAccountId = "abc",
            currency = "eur",
            amount = BigDecimal.ZERO,
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(mapOf(
            PROPERTY_DEBIT_ACCOUNT_ID to ERROR_MESSAGE_ACCOUNT_ID,
            PROPERTY_CREDIT_ACCOUNT_ID to ERROR_MESSAGE_ACCOUNT_ID,
            PROPERTY_CURRENCY to ERROR_MESSAGE_CURRENCY,
            PROPERTY_AMOUNT to ERROR_MESSAGE_AMOUNT,
        ), error.fieldErrors)

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }
}

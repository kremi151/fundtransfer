package lu.mkremer.fundstransfer

import lu.mkremer.fundstransfer.datamodel.dto.ValidationErrorDTO
import lu.mkremer.fundstransfer.datamodel.jpa.Account
import lu.mkremer.fundstransfer.datamodel.request.CreateAccountRequest
import lu.mkremer.fundstransfer.repository.AccountRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus

/**
 * Integration tests to validate user inputs for the account controller.
 * Successful validations are covered by [TransactionTests].
 */
class AccountValidationTests: AbstractIntegrationTest() {

    companion object {
        private const val GENERAL_ERROR_MESSAGE = "At least one submitted property has an invalid value"

        private const val ERROR_MESSAGE_CURRENCY = "The currency is not an uppercase char sequence of 3 letters, or not supported"

        private const val PROPERTY_CURRENCY = "currency"
    }

    @MockBean
    private lateinit var accountRepository: AccountRepository

    @BeforeEach
    fun setupStaticExchangeRates() {
        // For the sake of these tests, we just specify some static exchange
        // rates to make the currency validation work
        mockExchangeRates(mapOf("EUR" to "1.0".toBigDecimal()))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Euro", "eur", "EURO", "eu", "EU", "XYZ"])
    fun testCreateAccountWithInvalidCurrency(currency: String) {
        val error = attemptToCreateAccount(
            currency = currency,
        )
        assertEquals(GENERAL_ERROR_MESSAGE, error.message)
        assertEquals(
            mapOf(PROPERTY_CURRENCY to ERROR_MESSAGE_CURRENCY),
            error.fieldErrors,
        )

        // No account should have been updated or created
        verify(accountRepository, never()).save(any())
        verify(accountRepository, never()).saveAll(any<List<Account>>())
    }

    private fun attemptToCreateAccount(currency: String): ValidationErrorDTO {
        val request = CreateAccountRequest(
            currency = currency,
        )
        val response = restTemplate.postForEntity("/account", request, ValidationErrorDTO::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode, "Status code must be 400 Bad Request")

        val validationError = response.body
        assertNotNull(validationError, "Response body must not be null")

        // Tell the Kotlin compiler that we are sure that account cannot be null here
        return validationError!!
    }

}
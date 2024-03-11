package lu.mkremer.fundstransfer

import lu.mkremer.fundstransfer.datamodel.dto.ValidationErrorDTO
import lu.mkremer.fundstransfer.datamodel.jpa.Account
import lu.mkremer.fundstransfer.datamodel.request.CreateAccountRequest
import lu.mkremer.fundstransfer.repository.AccountRepository
import org.junit.jupiter.api.Assertions
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

        private const val ERROR_MESSAGE_CURRENCY = "The currency must be 3 letters in uppercase format"

        private const val PROPERTY_CURRENCY = "currency"
    }

    @MockBean
    private lateinit var accountRepository: AccountRepository

    @ParameterizedTest
    @ValueSource(strings = ["", "Euro", "eur", "EURO", "eu", "EU"])
    fun testCreateAccountWithInvalidCurrency(currency: String) {
        val error = attemptToCreateAccount(
            currency = currency,
        )
        Assertions.assertEquals(error.message, GENERAL_ERROR_MESSAGE)
        Assertions.assertEquals(
            error.fieldErrors,
            mapOf(PROPERTY_CURRENCY to ERROR_MESSAGE_CURRENCY)
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
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.statusCode, "Status code must be 400 Bad Request")

        val validationError = response.body
        Assertions.assertNotNull(validationError, "Response body must not be null")

        // Tell the Kotlin compiler that we are sure that account cannot be null here
        return validationError!!
    }

}
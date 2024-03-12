package lu.mkremer.fundstransfer

import lu.mkremer.fundstransfer.datamodel.dto.ValidationErrorDTO
import lu.mkremer.fundstransfer.datamodel.exchanger.ExchangeRates
import lu.mkremer.fundstransfer.datamodel.request.AccountBalanceRequest
import lu.mkremer.fundstransfer.datamodel.request.MoneyTransferRequest
import lu.mkremer.fundstransfer.service.ExchangeRateSynchronizer
import lu.mkremer.fundstransfer.service.FundTransferService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.io.IOException
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

/**
 * Common base class for integration tests, which takes case about configuring
 * the Tomcat port and setting the test profile.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    @LocalServerPort
    private var localPort: Int = 0

    @MockBean
    private lateinit var exchangeRateSynchronizer: ExchangeRateSynchronizer

    @Autowired
    private lateinit var fundTransferService: FundTransferService

    private lateinit var _restTemplate: TestRestTemplate

    protected val restTemplate: TestRestTemplate
        get() = _restTemplate

    @BeforeEach
    final fun initCommon() {
        _restTemplate = TestRestTemplate(
            RestTemplateBuilder()
                .rootUri("http://localhost:${localPort}")
        )
    }

    //
    // Test utilities
    //

    private fun updateExchangeRates() {
        // Update exchange rates based on mocked data
        // Since we don't actually contact an external service in the tests, this should not block the current thread
        // for too long.
        // Still, let's make sure we don't wait forever since this will still be performed asynchronously.
        fundTransferService
            .updateExchangeRates()
            .exceptionally {
                // Ignore exception if it is the one thrown in mockFailedExchangeRatesFetch
                if (it.cause !is SimulatedIOException) {
                    throw it
                }
            }
            .get(1, TimeUnit.SECONDS)
    }

    /**
     * Specifies a mocked set of exchange rates to be used by the system
     */
    protected final fun mockExchangeRates(rates: Map<String, Double>) {
        Mockito.`when`(exchangeRateSynchronizer.fetch()).thenReturn(ExchangeRates(rates))
        updateExchangeRates()
    }

    /**
     * Simulates a failed fetch of exchange rates, causing the system to throw
     * [lu.mkremer.fundstransfer.exception.ServiceNotReadyException] when trying to access
     */
    protected final fun mockFailedExchangeRatesFetch() {
        Mockito.`when`(exchangeRateSynchronizer.fetch()).thenAnswer {
            throw SimulatedIOException("This is an expected exception to simulate a failure when fetching exchange rates")
        }
        updateExchangeRates()
    }

    private class SimulatedIOException(message: String): IOException(message)

    //
    // Common methods for performing REST API requests
    //

    protected final fun attemptToDepositMoney(accountId: String, amount: BigDecimal, currency: String): ValidationErrorDTO {
        val request = AccountBalanceRequest(
            accountId = accountId,
            amount = amount,
            currency = currency,
        )
        val response = restTemplate.postForEntity("/transaction/deposit", request, ValidationErrorDTO::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode, "Status code must be 400 Bad Request")

        val body = response.body
        assertNotNull(body, "Response body must not be null")

        // Tell the Kotlin compiler that we are sure that account cannot be null here
        return body!!
    }

    protected final fun attemptToWithdrawMoney(accountId: String, amount: BigDecimal, currency: String): ValidationErrorDTO {
        val request = AccountBalanceRequest(
            accountId = accountId,
            amount = amount,
            currency = currency,
        )
        val response = restTemplate.postForEntity("/transaction/withdraw", request, ValidationErrorDTO::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode, "Status code must be 400 Bad Request")

        val body = response.body
        assertNotNull(body, "Response body must not be null")

        // Tell the Kotlin compiler that we are sure that body cannot be null here
        return body!!
    }

    protected final fun attemptToTransferMoney(debitAccountId: String, creditAccountId: String, amount: BigDecimal, currency: String): ValidationErrorDTO {
        val request = MoneyTransferRequest(
            debitAccountId = debitAccountId,
            creditAccountId = creditAccountId,
            amount = amount,
            currency = currency,
        )
        val response = restTemplate.postForEntity("/transaction/transfer", request, ValidationErrorDTO::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode, "Status code must be 400 Bad Request")

        val body = response.body
        assertNotNull(body)

        // Tell the Kotlin compiler that we are sure that body cannot be null here
        return body!!
    }

}

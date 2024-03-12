package lu.mkremer.fundstransfer

import lu.mkremer.fundstransfer.datamodel.exchanger.ExchangeRates
import lu.mkremer.fundstransfer.service.ExchangeRateSynchronizer
import lu.mkremer.fundstransfer.service.FundTransferService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.test.context.ActiveProfiles
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

    @AfterEach
    final fun cleanupExchangeRates() {
        // Cleanup any previously mocked exchange rates (to avoid
        // interference between tests)
        mockExchangeRates(mapOf())
    }

    protected final fun mockExchangeRates(rates: Map<String, Double>) {
        Mockito.`when`(exchangeRateSynchronizer.fetch()).thenReturn(ExchangeRates(rates))
        // Update exchange rates based on mocked data
        // Since we don't actually contact an external service in the tests, this should not block the current thread
        // for too long.
        // Still, let's make sure we don't wait forever since this will still be performed asynchronously.
        fundTransferService.updateExchangeRates().get(1, TimeUnit.SECONDS)
    }

}

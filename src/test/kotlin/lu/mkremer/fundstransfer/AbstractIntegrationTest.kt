package lu.mkremer.fundstransfer

import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.test.context.ActiveProfiles

/**
 * Common base class for integration tests, which takes case about configuring
 * the Tomcat port and setting the test profile.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    @LocalServerPort
    private var localPort: Int = 0

    private lateinit var _restTemplate: TestRestTemplate

    protected val restTemplate: TestRestTemplate
        get() = _restTemplate

    @BeforeEach
    fun initCommon() {
        _restTemplate = TestRestTemplate(
            RestTemplateBuilder()
                .rootUri("http://localhost:${localPort}")
        )
    }

}

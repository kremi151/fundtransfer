package lu.mkremer.fundstransfer.service.impl

import lu.mkremer.fundstransfer.datamodel.exchanger.ExchangeRates
import lu.mkremer.fundstransfer.service.ExchangeRateSynchronizer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.lang.invoke.MethodHandles

/**
 * An implementation of [ExchangeRateSynchronizer] that uses the API of https://www.frankfurter.app/
 */
@Service
@Profile("!test") // Do not load in tests
class ExchangeRateSynchronizerFrankfurter(
    private val restTemplate: RestTemplate,
): ExchangeRateSynchronizer {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }

    override fun fetch(): ExchangeRates {
        LOGGER.info("Fetching exchange rates...")
        val response = restTemplate.getForEntity("https://www.frankfurter.app/latest", ExchangeResponse::class.java)
        if (!response.statusCode.is2xxSuccessful) {
            throw IOException("Unable to update exchange rates, received HTTP status ${response.statusCode.value()}")
        }

        val body = response.body
            ?: throw IOException("Unable to update exchange rates, received empty response body")

        val rates = body.rates + mapOf(body.base to body.amount)

        LOGGER.info("Fetched exchange rates")

        return ExchangeRates.fromDoubleRates(rates)
    }

    private data class ExchangeResponse(
        val amount: Double,
        val base: String,
        val date: String,
        val rates: Map<String, Double>,
    )

}

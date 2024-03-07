package lu.mkremer.fundstransfer.service.impl

import lu.mkremer.fundstransfer.exception.ServiceNotReadyException
import lu.mkremer.fundstransfer.exception.UnsupportedCurrencyException
import lu.mkremer.fundstransfer.service.CurrencyExchanger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * An implementation of [CurrencyExchanger] that uses the API of https://www.frankfurter.app/
 */
@Service
class CurrencyExchangerFrankfurter(
    private val restTemplate: RestTemplate,
): CurrencyExchanger {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }

    private var rates: Map<String, Double>? = null

    override fun supportsCurrency(currency: String): Boolean = rates?.containsKey(currency) ?: false

    override fun convert(
        fromAmount: BigDecimal,
        fromCurrency: String,
        toCurrency: String
    ): BigDecimal = rates?.let {
        if (fromCurrency == toCurrency) {
            fromAmount
        } else {
            // First, we need to find a common ground before we can do the conversion.
            // For this, we choose the base currency of the external service.
            val rateOfSourceCurrency = it[fromCurrency] ?: throw UnsupportedCurrencyException(fromCurrency)
            val baseAmount = fromAmount.divide(BigDecimal(rateOfSourceCurrency), 4, RoundingMode.HALF_UP)

            // Second, we can now convert the amount from the base currency to the
            // target currency
            val rateOfTargetCurrency = it[toCurrency] ?: throw UnsupportedCurrencyException(toCurrency)
            baseAmount.multiply(BigDecimal(rateOfTargetCurrency))
        }
    } ?: throw ServiceNotReadyException("Exchange rates are not loaded yet")

    override fun update() {
        LOGGER.info("Updating exchange rates...")
        val response = restTemplate.getForEntity("https://www.frankfurter.app/latest", ExchangeResponse::class.java)
        if (!response.statusCode.is2xxSuccessful) {
            throw IOException("Unable to update exchange rates, received HTTP status ${response.statusCode.value()}")
        }

        val body = response.body
            ?: throw IOException("Unable to update exchange rates, received empty response body")

        rates = body.rates + mapOf(body.base to body.amount)

        LOGGER.info("Updated exchange rates")
    }

    private data class ExchangeResponse(
        val amount: Double,
        val base: String,
        val date: String,
        val rates: Map<String, Double>,
    )

}
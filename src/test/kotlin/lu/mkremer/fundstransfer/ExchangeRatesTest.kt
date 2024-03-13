package lu.mkremer.fundstransfer

import lu.mkremer.fundstransfer.datamodel.dto.MonetaryAmountDTO
import lu.mkremer.fundstransfer.datamodel.exchanger.ExchangeRates
import lu.mkremer.fundstransfer.util.Assertions.assertComparableEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

class ExchangeRatesTest {

    companion object {
        // Mocked values for exchange rates
        private val EUR_TO_EUR = BigDecimal.ONE
        private val EUR_TO_JPY = "160.5".toBigDecimal()
        private val EUR_TO_CHF = "0.96".toBigDecimal()
    }

    @ParameterizedTest
    @CsvSource(value = [
        "EUR, 100.0,                CHF, 96.0",
        "EUR, 0.0,                  CHF, 0.0",
        "JPY, 0.0,                  CHF, 0.0",
        "JPY, 0.0,                  EUR, 0.0",
        "JPY, 139.0,                CHF, 0.83140186944",
        "JPY, 142.0,                CHF, 0.84934579392",
        "CHF, 1806.95,              JPY, 302099.4531249465",
        "CHF, 20240313111514754.85, EUR, 21083659491161202.96875"
    ])
    fun testConversion(
        sourceCurrency: String,
        sourceAmount: String,
        targetCurrency: String,
        expectedResult: String,
    ) {
        val exchangeRates = ExchangeRates(mapOf(
            "EUR" to EUR_TO_EUR,
            "JPY" to EUR_TO_JPY,
            "CHF" to EUR_TO_CHF,
        ))
        val result = exchangeRates.convert(
            amount = MonetaryAmountDTO(sourceAmount.toBigDecimal(), sourceCurrency),
            toCurrency = targetCurrency,
        )
        assertComparableEquals(expectedResult.toBigDecimal(), result)
    }

    @Test
    fun testExchangeRatesPrecision() {
        val rawExchangeRates = mapOf(
            "EUR" to 1.0,
            "CAD" to 1.4739,
            "USD" to 1.0916,
            "CHF" to 0.9588,
        )
        val parsed = ExchangeRates.fromDoubleRates(rawExchangeRates)
        with(parsed) {
            assertEquals(setOf("EUR", "CAD", "USD", "CHF"), rates.keys, "Parsed rates must contain the expected currencies")
            assertEquals("1.0", rates["EUR"]?.toString())
            assertEquals("1.4739", rates["CAD"]?.toString())
            assertEquals("1.0916", rates["USD"]?.toString())
            assertEquals("0.9588", rates["CHF"]?.toString())
        }
    }

}
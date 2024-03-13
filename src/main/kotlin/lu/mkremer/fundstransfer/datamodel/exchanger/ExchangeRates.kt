package lu.mkremer.fundstransfer.datamodel.exchanger

import lu.mkremer.fundstransfer.datamodel.dto.MonetaryAmountDTO
import lu.mkremer.fundstransfer.exception.UnsupportedCurrenciesException
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * An immutable wrapper for exchange rates, which is able to perform currency
 * conversions on its own
 */
class ExchangeRates(
    private val rates: Map<String, BigDecimal>,
) {

    /**
     * Verifies whether the given [currency] is supported
     */
    fun supportsCurrency(currency: String): Boolean = rates.containsKey(currency)

    /**
     * Converts an amount of money [amount] to an equivalent amount in
     * currency [toCurrency]
     */
    fun convert(
        amount: MonetaryAmountDTO,
        toCurrency: String
    ): BigDecimal {
        return if (amount.currency == toCurrency) {
            amount.amount
        } else {
            // First, we need to find a common ground before we can do the conversion.
            // For this, we choose the base currency of the external service.
            val rateOfDebitCurrency = rates[amount.currency] ?: throw UnsupportedCurrenciesException(amount.currency)
            val baseAmount = amount.amount.divide(rateOfDebitCurrency, 9, RoundingMode.HALF_UP)

            // Second, we can now convert the amount from the base currency to the
            // target currency
            val rateOfCreditCurrency = rates[toCurrency] ?: throw UnsupportedCurrenciesException(toCurrency)
            baseAmount.multiply(rateOfCreditCurrency)
        }
    }

    companion object {

        fun fromDoubleRates(rates: Map<String, Double>): ExchangeRates = ExchangeRates(
            rates.mapValues {
                // It is safer to first convert the double to a string, and then pass this string
                // to the constructor of BigDecimal. Like this, we can have the most accurate
                // representation of the decimal number in a BigDecimal, and we avoid rounding
                // issues when passing the double directly to the constructor of BigDecimal.
                // e.g. BigDecimal(0.96)   -> 0.95999999999999996447286321199499070644378662109375
                //      BigDecimal("0.96") -> 0.96
                BigDecimal(it.value.toString())
            }
        )

    }

}

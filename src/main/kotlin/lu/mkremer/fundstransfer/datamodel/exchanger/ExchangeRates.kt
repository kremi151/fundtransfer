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
    private val rates: Map<String, Double>,
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
            val baseAmount = amount.amount.divide(BigDecimal(rateOfDebitCurrency), 4, RoundingMode.HALF_UP)

            // Second, we can now convert the amount from the base currency to the
            // target currency
            val rateOfCreditCurrency = rates[toCurrency] ?: throw UnsupportedCurrenciesException(toCurrency)
            baseAmount.multiply(BigDecimal(rateOfCreditCurrency))
        }
    }

}

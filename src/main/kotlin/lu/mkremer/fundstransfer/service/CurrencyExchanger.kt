package lu.mkremer.fundstransfer.service

import java.math.BigDecimal

/**
 * An interface that exposes the methods required to perform currency exchanges
 * between different currencies.
 */
interface CurrencyExchanger { // TODO: Use better naming?

    /**
     * Verifies whether the implementation supports the given [currency]
     */
    fun supportsCurrency(currency: String): Boolean

    /**
     * Converts an amount of [fromAmount] in currency [fromCurrency] to an
     * equivalent amount in currency [toCurrency]
     */
    fun convert(fromAmount: BigDecimal, fromCurrency: String, toCurrency: String): BigDecimal

    /**
     * Triggers an update of the exchange rates
     */
    fun update()

}

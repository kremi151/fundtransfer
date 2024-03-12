package lu.mkremer.fundstransfer.service

import lu.mkremer.fundstransfer.datamodel.dto.MonetaryAmountDTO
import java.util.concurrent.CompletableFuture

interface FundTransferService {

    /**
     * Convert an amount of money to a target currency
     */
    fun convert(monetaryAmount: MonetaryAmountDTO, targetCurrency: String): MonetaryAmountDTO

    /**
     * Asynchronously updates the exchange rates
     */
    fun updateExchangeRates(): CompletableFuture<Unit>

    /**
     * Verifies whether the given [currency] is supported
     */
    fun supportsCurrency(currency: String): Boolean

    /**
     * Specifies whether the service is ready, i.e. exchange rates are loaded
     */
    val ready: Boolean

}

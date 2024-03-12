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

}

package lu.mkremer.fundstransfer.service

import lu.mkremer.fundstransfer.datamodel.dto.MonetaryAmountDTO

interface FundTransferService {

    /**
     * Convert an amount of money to a target currency
     */
    fun convert(monetaryAmount: MonetaryAmountDTO, targetCurrency: String): MonetaryAmountDTO

}

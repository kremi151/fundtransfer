package lu.mkremer.fundstransfer.datamodel.dto

import java.math.BigDecimal

/**
 * A DTO describing an amount of money in a given currency
 */
data class MonetaryAmountDTO(
    val amount: BigDecimal,
    val currency: String,
)

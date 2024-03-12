package lu.mkremer.fundstransfer.exception

import java.math.BigDecimal

/**
 * This exception is thrown in cases where money is attempted to be withdrawn
 * or transferred from an account whose balance is not sufficient.
 */
class InsufficientBalanceException(
    val accountId: String,
    val missing: BigDecimal,
    val currency: String,
): Exception()

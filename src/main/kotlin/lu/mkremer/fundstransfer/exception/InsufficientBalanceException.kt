package lu.mkremer.fundstransfer.exception

import java.math.BigDecimal

class InsufficientBalanceException(
    val accountId: String,
    val missing: BigDecimal,
    val currency: String,
): Exception()

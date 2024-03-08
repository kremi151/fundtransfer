package lu.mkremer.fundstransfer.datamodel.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import lu.mkremer.fundstransfer.validation.AccountId
import lu.mkremer.fundstransfer.validation.Currency
import java.math.BigDecimal

@Schema(description = "The request to deposit money in an account")
class DepositMoneyRequest(
    @Schema(description = "The account to deposit money in", required = true)
    @AccountId // TODO: Write test for validation
    val accountId: String,

    @Schema(description = "The amount of money to deposit on the account", required = true)
    @DecimalMin(value = "0.0", inclusive = false, message = "The amount must be strictly positive") // TODO: Write test for validation
    val amount: BigDecimal,

    @Schema(description = "The currency of the money to deposit", required = true)
    @Currency // TODO: Write test for validation
    val currency: String,
)

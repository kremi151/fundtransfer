package lu.mkremer.fundstransfer.datamodel.request

import io.swagger.v3.oas.annotations.media.Schema
import lu.mkremer.fundstransfer.validation.AccountId
import lu.mkremer.fundstransfer.validation.Currency
import lu.mkremer.fundstransfer.validation.PositiveAmount
import java.math.BigDecimal

@Schema(description = "The request to deposit or withdraw money in/from an account")
class AccountBalanceRequest(
    @Schema(description = "The target account ID", required = true)
    @AccountId
    val accountId: String,

    @Schema(description = "The amount of money to deposit or withdraw", required = true)
    @PositiveAmount
    val amount: BigDecimal,

    @Schema(description = "The currency of the money to deposit or withdraw", required = true)
    @Currency
    val currency: String,
)

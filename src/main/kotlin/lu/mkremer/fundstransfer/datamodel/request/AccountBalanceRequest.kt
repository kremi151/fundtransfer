package lu.mkremer.fundstransfer.datamodel.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import lu.mkremer.fundstransfer.validation.AccountId
import lu.mkremer.fundstransfer.validation.Currency
import java.math.BigDecimal

@Schema(description = "The request to deposit or withdraw money in/from an account")
class AccountBalanceRequest(
    @Schema(description = "The target account ID", required = true)
    @AccountId // TODO: Write test for validation
    val accountId: String,

    @Schema(description = "The amount of money to deposit or withdraw", required = true)
    @DecimalMin(value = "0.0", inclusive = false, message = "The amount must be strictly positive") // TODO: Write test for validation
    val amount: BigDecimal,

    @Schema(description = "The currency of the money to deposit or withdraw", required = true)
    @Currency // TODO: Write test for validation
    val currency: String,
)

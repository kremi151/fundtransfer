package lu.mkremer.fundstransfer.datamodel.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import lu.mkremer.fundstransfer.validation.AccountId
import lu.mkremer.fundstransfer.validation.Currency
import java.math.BigDecimal

@Schema(description = "The request to transfer money from one account to another")
class MoneyTransferRequest(

    @Schema(description = "The debit account ID to withdraw the money from", required = true)
    @AccountId // TODO: Write test for validation
    val debitAccountId: String,

    @Schema(description = "The credit account ID to deposit the money in", required = true)
    @AccountId // TODO: Write test for validation
    val creditAccountId: String,

    @Schema(description = "The amount of money to transfer", required = true)
    @DecimalMin(value = "0.0", inclusive = false, message = "The amount must be strictly positive") // TODO: Write test for validation
    val amount: BigDecimal,

    @Schema(description = "The currency of the money to transfer", required = true)
    @Currency // TODO: Write test for validation
    val currency: String,

)
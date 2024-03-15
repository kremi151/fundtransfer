package lu.mkremer.fundstransfer.datamodel.request

import io.swagger.v3.oas.annotations.media.Schema
import lu.mkremer.fundstransfer.validation.AccountId
import lu.mkremer.fundstransfer.validation.Currency
import lu.mkremer.fundstransfer.validation.PositiveAmount
import java.math.BigDecimal

@Schema(description = "The request to transfer money from one account to another")
class MoneyTransferRequest(

    @Schema(description = "The debit account ID to withdraw the money from", required = true)
    @AccountId
    val debitAccountId: String,

    @Schema(description = "The credit account ID to deposit the money in", required = true)
    @AccountId
    val creditAccountId: String,

    @Schema(description = "The amount of money to transfer", required = true)
    @PositiveAmount
    val amount: BigDecimal,

    @Schema(description = "The currency of the money to transfer", required = true)
    @Currency
    val currency: String, // TODO: Check if actually needed

)
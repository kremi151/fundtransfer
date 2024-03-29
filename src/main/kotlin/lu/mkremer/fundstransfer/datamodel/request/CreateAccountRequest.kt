package lu.mkremer.fundstransfer.datamodel.request

import io.swagger.v3.oas.annotations.media.Schema
import lu.mkremer.fundstransfer.validation.Currency

@Schema(description = "The request for creating a new account")
class CreateAccountRequest(
    @Schema(description = "The currency of the money that this account will hold", required = true)
    @Currency
    val currency: String,
)

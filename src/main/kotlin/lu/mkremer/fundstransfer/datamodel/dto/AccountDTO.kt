package lu.mkremer.fundstransfer.datamodel.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "A description about an existing account")
data class AccountDTO(
    @Schema(description = "The id of the accounts", required = true)
    val id: String,

    @Schema(description = "The currency of this account", required = true)
    val currency: String,

    @Schema(description = "The current balance on this account", required = true)
    val balance: BigDecimal,
)

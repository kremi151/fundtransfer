package lu.mkremer.fundstransfer.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.dto.ValidationErrorDTO
import lu.mkremer.fundstransfer.datamodel.request.DepositMoneyRequest
import lu.mkremer.fundstransfer.service.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(
    name = "transaction",
    description = "Controller for all kinds of money transactions",
)
@RequestMapping("/transaction")
class TransactionController {

    @Autowired
    private lateinit var transactionService: TransactionService

    @PostMapping("/deposit")
    @Operation(summary = "Deposit money in an account")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "The amount of money was deposited",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AccountDTO::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid input supplied",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ValidationErrorDTO::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Account not found",
                content = [Content()]
            ),
        ])
    fun depositMoney(@Valid @RequestBody request: DepositMoneyRequest): AccountDTO {
        return transactionService.depositMoney(request)
    }

}

package lu.mkremer.fundstransfer.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.dto.ValidationErrorDTO
import lu.mkremer.fundstransfer.datamodel.request.CreateAccountRequest
import lu.mkremer.fundstransfer.service.AccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(
    name = "account",
    description = "Controller for all operations concerning accounts",
)
@RequestMapping("/account")
class AccountController {

    @Autowired
    private lateinit var accountService: AccountService

    @PostMapping
    @Operation(summary = "Creates a new account")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "The account was created",
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
        ])
    @ResponseStatus(HttpStatus.CREATED)
    fun createAccount(@Valid @RequestBody request: CreateAccountRequest): AccountDTO {
        return accountService.createAccount(request)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieves information about an account")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "The account was found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = AccountDTO::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "The account was not found",
                content = [Content()],
            ),
        ])
    fun getAccount(
        @Parameter(description = "The account ID") @PathVariable("id") id: Int, // TODO: Validation
    ): ResponseEntity<AccountDTO> {
        return accountService.getAccount(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

}
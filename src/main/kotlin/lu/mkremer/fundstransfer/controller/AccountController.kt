package lu.mkremer.fundstransfer.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.request.CreateAccountRequest
import lu.mkremer.fundstransfer.service.AccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
    fun createAccount(@RequestBody request: CreateAccountRequest): AccountDTO {
        return accountService.createAccount(request)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieves information about an account")
    fun getAccount(
        @Parameter(description = "The account ID") @PathVariable("id") id: Int,
    ): ResponseEntity<AccountDTO> {
        return accountService.getAccount(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

}
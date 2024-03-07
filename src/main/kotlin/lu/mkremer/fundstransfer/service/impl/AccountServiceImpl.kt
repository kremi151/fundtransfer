package lu.mkremer.fundstransfer.service.impl

import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.jpa.Account
import lu.mkremer.fundstransfer.datamodel.request.CreateAccountRequest
import lu.mkremer.fundstransfer.extension.asDTO
import lu.mkremer.fundstransfer.repository.AccountRepository
import lu.mkremer.fundstransfer.service.AccountService
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class AccountServiceImpl(
    private val accountRepository: AccountRepository,
): AccountService {

    private val random = SecureRandom() // TODO: Think about how this could work in a multi instance environment

    override fun createAccount(request: CreateAccountRequest): AccountDTO {
        val account = Account(
            id = random.nextInt(1000000000), // This will generate an id from 0 to 999999999 (inclusive)
            currency = request.currency,
        )
        return accountRepository
            .save(account)
            .asDTO()
    }

    override fun getAccount(id: Int): AccountDTO? = accountRepository
        .findById(id)
        .map { it.asDTO() }
        .orElse(null)

}

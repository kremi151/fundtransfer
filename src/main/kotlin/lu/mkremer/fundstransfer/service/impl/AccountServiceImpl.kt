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

    private val random = SecureRandom()

    override fun createAccount(request: CreateAccountRequest): AccountDTO {
        val account = Account(
            id = findUniqueId(),
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

    private fun findUniqueId(): Int {
        // Let's give ourselves up to 100 attempts to find a unique ID to avoid
        // using an infinite loop here which may never return
        for (i in 0 until 100) {
            // This will generate an id from 1 to 999999999 (inclusive)
            val id = 1 + random.nextInt(999999999)

            if (!accountRepository.existsById(id)) {
                return id
            }
        }

        throw IllegalStateException("No unique account ID could be allocated")
    }

}

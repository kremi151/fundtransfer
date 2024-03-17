package lu.mkremer.fundstransfer.service.impl

import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.dto.MonetaryAmountDTO
import lu.mkremer.fundstransfer.datamodel.request.AccountBalanceRequest
import lu.mkremer.fundstransfer.datamodel.request.MoneyTransferRequest
import lu.mkremer.fundstransfer.exception.IllegalMoneyTransferException
import lu.mkremer.fundstransfer.exception.InsufficientBalanceException
import lu.mkremer.fundstransfer.extension.asDTO
import lu.mkremer.fundstransfer.repository.AccountRepository
import lu.mkremer.fundstransfer.service.FundTransferService
import lu.mkremer.fundstransfer.service.TransactionService
import org.h2.mvstore.MVStoreException
import org.hibernate.StaleObjectStateException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TransactionServiceImpl(
    private val accountRepository: AccountRepository,
    private val fundTransferService: FundTransferService,
): TransactionService {

    @Transactional
    @Retryable(
        retryFor = [
            StaleObjectStateException::class,
            MVStoreException::class,
        ],
        backoff = Backoff(delay = 500, multiplier = 2.0),
    )
    override fun depositMoney(request: AccountBalanceRequest): AccountDTO {
        val id = request.accountId.toInt() // Validation performed by Hibernate Validator (See @AccountId annotation)

        val account = accountRepository.findByIdWithLock(id).orElseThrow()

        val converted = fundTransferService.convert(
            monetaryAmount = MonetaryAmountDTO(
                amount = request.amount,
                currency = request.currency,
            ),
            targetCurrency = account.currency,
        )

        account.balance = account.balance.plus(converted.amount)

        return accountRepository
            .save(account)
            .asDTO()
    }

    @Transactional
    @Retryable(
        retryFor = [
            StaleObjectStateException::class,
            MVStoreException::class,
        ],
        backoff = Backoff(delay = 500, multiplier = 2.0),
    )
    override fun withdrawMoney(request: AccountBalanceRequest): AccountDTO {
        val id = request.accountId.toInt() // Validation performed by Hibernate Validator (See @AccountId annotation)

        val account = accountRepository.findByIdWithLock(id).orElseThrow()

        val converted = fundTransferService.convert(
            monetaryAmount = MonetaryAmountDTO(
                amount = request.amount,
                currency = request.currency,
            ),
            targetCurrency = account.currency,
        )

        if (account.balance < converted.amount) {
            throw InsufficientBalanceException(
                accountId = request.accountId,
                missing = converted.amount.minus(account.balance),
                currency = account.currency,
            )
        }

        account.balance = account.balance.minus(converted.amount)

        return accountRepository
            .save(account)
            .asDTO()
    }

    @Transactional
    @Retryable(
        retryFor = [
            StaleObjectStateException::class,
            MVStoreException::class,
        ],
        backoff = Backoff(delay = 500, multiplier = 2.0),
    )
    override fun transferMoney(request: MoneyTransferRequest): AccountDTO {
        // Validations performed by Hibernate Validator (See @AccountId annotation)
        val debitAccountId = request.debitAccountId.toInt()
        val creditAccountId = request.creditAccountId.toInt()

        if (debitAccountId == creditAccountId) {
            throw IllegalMoneyTransferException("Transferring money from and to the same account is not allowed")
        }

        var debitAccount = accountRepository.findByIdWithLock(debitAccountId).orElseThrow()
        val creditAccount = accountRepository.findByIdWithLock(creditAccountId).orElseThrow()

        val withdrawnMoney = fundTransferService.convert(
            monetaryAmount = MonetaryAmountDTO(
                amount = request.amount,
                currency = request.currency,
            ),
            targetCurrency = debitAccount.currency,
        )

        if (debitAccount.balance < withdrawnMoney.amount) {
            throw InsufficientBalanceException(
                accountId = request.debitAccountId,
                missing = withdrawnMoney.amount.minus(debitAccount.balance),
                currency = debitAccount.currency,
            )
        }

        val depositedMoney = fundTransferService.convert(
            monetaryAmount = MonetaryAmountDTO(
                amount = withdrawnMoney.amount,
                currency = withdrawnMoney.currency,
            ),
            targetCurrency = creditAccount.currency,
        )

        debitAccount.balance = debitAccount.balance.minus(withdrawnMoney.amount)
        creditAccount.balance = creditAccount.balance.plus(depositedMoney.amount)

        debitAccount = accountRepository.save(debitAccount)
        accountRepository.save(creditAccount)

        return debitAccount.asDTO()
    }

}
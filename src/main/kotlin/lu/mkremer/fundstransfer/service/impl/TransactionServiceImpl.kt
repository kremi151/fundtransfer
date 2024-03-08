package lu.mkremer.fundstransfer.service.impl

import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.dto.MonetaryAmountDTO
import lu.mkremer.fundstransfer.datamodel.request.AccountBalanceRequest
import lu.mkremer.fundstransfer.datamodel.request.MoneyTransferRequest
import lu.mkremer.fundstransfer.exception.InsufficientBalanceException
import lu.mkremer.fundstransfer.extension.asDTO
import lu.mkremer.fundstransfer.repository.AccountRepository
import lu.mkremer.fundstransfer.service.FundTransferService
import lu.mkremer.fundstransfer.service.TransactionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TransactionServiceImpl(
    private val accountRepository: AccountRepository,
    private val fundTransferService: FundTransferService,
): TransactionService {

    @Transactional
    override fun depositMoney(request: AccountBalanceRequest): AccountDTO {
        val id = request.accountId.toInt() // Validation performed by Hibernate Validator (See @AccountId annotation)

        val account = accountRepository.findById(id).orElseThrow()

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
    override fun withdrawMoney(request: AccountBalanceRequest): AccountDTO {
        val id = request.accountId.toInt() // Validation performed by Hibernate Validator (See @AccountId annotation)

        val account = accountRepository.findById(id).orElseThrow()

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
            ) // TODO: Test
        }

        account.balance = account.balance.minus(converted.amount)

        return accountRepository
            .save(account)
            .asDTO()
    }

    @Transactional
    override fun transferMoney(request: MoneyTransferRequest): AccountDTO {
        // Validations performed by Hibernate Validator (See @AccountId annotation)
        val sourceId = request.sourceAccountId.toInt()
        val targetId = request.targetAccountId.toInt()

        var sourceAccount = accountRepository.findById(sourceId).orElseThrow()
        val targetAccount = accountRepository.findById(targetId).orElseThrow()

        val withdrawnMoney = fundTransferService.convert(
            monetaryAmount = MonetaryAmountDTO(
                amount = request.amount,
                currency = request.currency,
            ),
            targetCurrency = sourceAccount.currency,
        )

        if (sourceAccount.balance < withdrawnMoney.amount) {
            throw InsufficientBalanceException(
                accountId = request.sourceAccountId,
                missing = withdrawnMoney.amount.minus(sourceAccount.balance),
                currency = sourceAccount.currency,
            ) // TODO: Test
        }

        val depositedMoney = fundTransferService.convert(
            monetaryAmount = MonetaryAmountDTO(
                amount = withdrawnMoney.amount,
                currency = withdrawnMoney.currency,
            ),
            targetCurrency = targetAccount.currency,
        )

        sourceAccount.balance = sourceAccount.balance.minus(withdrawnMoney.amount)
        targetAccount.balance = targetAccount.balance.plus(depositedMoney.amount)

        sourceAccount = accountRepository.save(sourceAccount)
        accountRepository.save(targetAccount)

        return sourceAccount.asDTO()
    }

}
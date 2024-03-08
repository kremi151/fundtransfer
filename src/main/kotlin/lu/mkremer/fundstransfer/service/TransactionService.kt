package lu.mkremer.fundstransfer.service

import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.request.AccountBalanceRequest
import lu.mkremer.fundstransfer.datamodel.request.MoneyTransferRequest

/**
 * A service responsible for money operations on accounts
 */
interface TransactionService {

    /**
     * Performs a money deposit on an account.
     * @return The account after the money was deposited
     */
    fun depositMoney(request: AccountBalanceRequest): AccountDTO

    /**
     * Performs a money withdraw from an account.
     * @return The account after the money was withdrawn
     */
    fun withdrawMoney(request: AccountBalanceRequest): AccountDTO

    /**
     * Performs a money transfer between two accounts.
     * @return The source account after processing the money transfer
     */
    fun transferMoney(request: MoneyTransferRequest): AccountDTO

}

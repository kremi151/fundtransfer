package lu.mkremer.fundstransfer.service

import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.request.CreateAccountRequest

interface AccountService {

    /**
     * Creates a new account for the given input
     */
    fun createAccount(request: CreateAccountRequest): AccountDTO

    /**
     * Retrieves an account from the database based on the input id, if existing.
     * Otherwise, this returns null.
     */
    fun getAccount(id: Int): AccountDTO?

}

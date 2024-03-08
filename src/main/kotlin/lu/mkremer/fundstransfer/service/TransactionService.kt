package lu.mkremer.fundstransfer.service

import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.request.AccountBalanceRequest

interface TransactionService {

    fun depositMoney(request: AccountBalanceRequest): AccountDTO
    fun withdrawMoney(request: AccountBalanceRequest): AccountDTO

}

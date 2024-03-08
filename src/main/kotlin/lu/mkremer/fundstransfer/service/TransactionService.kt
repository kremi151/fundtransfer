package lu.mkremer.fundstransfer.service

import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.request.DepositMoneyRequest

interface TransactionService {

    fun depositMoney(request: DepositMoneyRequest): AccountDTO

}

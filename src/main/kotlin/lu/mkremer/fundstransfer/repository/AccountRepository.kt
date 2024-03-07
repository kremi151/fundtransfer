package lu.mkremer.fundstransfer.repository

import lu.mkremer.fundstransfer.datamodel.jpa.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository: JpaRepository<Account, Int>

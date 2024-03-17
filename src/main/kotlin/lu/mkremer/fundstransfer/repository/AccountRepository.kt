package lu.mkremer.fundstransfer.repository

import jakarta.persistence.LockModeType
import lu.mkremer.fundstransfer.datamodel.jpa.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface AccountRepository: JpaRepository<Account, Int> {

    /**
     * Functionally similar to [findById], but additionally acquiring a
     * pessimistic lock on the retrieved [Account].
     */
    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    @Query("select a from Account a where a.id = :id")
    fun findByIdWithLock(id: Int): Optional<Account>

}

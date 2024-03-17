package lu.mkremer.fundstransfer.datamodel.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Version
import java.math.BigDecimal

@Entity
class Account(
    @Id
    @Column(nullable = false)
    val id: Int,

    @Column(nullable = false, length = 3)
    var currency: String,

    @Column(nullable = false)
    var balance: BigDecimal = DEFAULT_BALANCE,

    @Version
    @Column(nullable = false)
    var version: Int? = null,
) {
    companion object {
        val DEFAULT_BALANCE: BigDecimal = BigDecimal.ZERO
    }
}

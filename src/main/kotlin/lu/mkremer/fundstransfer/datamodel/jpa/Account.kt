package lu.mkremer.fundstransfer.datamodel.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.math.BigDecimal

@Entity
class Account(
    @Id
    @Column(nullable = false)
    val id: Int,

    @Column(nullable = false, length = 3)
    var currency: String, // TODO: Use enum?

    @Column(nullable = false)
    var balance: BigDecimal = DEFAULT_BALANCE,
) {
    companion object {
        val DEFAULT_BALANCE: BigDecimal = BigDecimal.ZERO
    }
}

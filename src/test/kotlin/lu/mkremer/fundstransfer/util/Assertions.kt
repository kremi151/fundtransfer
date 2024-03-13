package lu.mkremer.fundstransfer.util

import org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure
import java.util.function.Supplier

/**
 * A set of custom JUnit assertions
 */
object Assertions {

    /**
     * Asserts that to [Comparable] objects are equal by calling [Comparable.compareTo],
     * and expecting a 0 return value.
     */
    fun <T: Comparable<T>> assertComparableEquals(expected: T, actual: T, supplyMessage: (() -> String)) {
        val comparison = actual.compareTo(expected)
        if (comparison != 0) {
            assertionFailure()
                .message(Supplier { supplyMessage() })
                .expected(expected)
                .actual(actual)
                .buildAndThrow()
        }
    }

    /**
     * Asserts that to [Comparable] objects are equal by calling [Comparable.compareTo],
     * and expecting a 0 return value.
     */
    fun <T: Comparable<T>> assertComparableEquals(expected: T, actual: T, message: String? = null) {
        assertComparableEquals(expected, actual) {
            message ?: "$actual is not equal to $expected after comparison"
        }
    }

}

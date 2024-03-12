package lu.mkremer.fundstransfer.exception

/**
 * An exception that is thrown when dealing with a currency that is either not
 * known, or unsupported
 */
class UnsupportedCurrenciesException(
    private val currencies: Set<String>,
): Exception() {

    /**
     * Convenience constructor for specifying one single currency
     */
    constructor(currency: String): this(setOf(currency))

    override val message: String
        get() = when {
            currencies.isEmpty() -> "Unsupported currencies"
            currencies.size == 1 -> "Unsupported currency: ${currencies.first()}"
            else -> "Unsupported currencies: ${currencies.joinToString(", ")}"
        }

}

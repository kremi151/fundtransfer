package lu.mkremer.fundstransfer.exception

/**
 * An exception that is thrown when dealing with a currency that is either not
 * known, or unsupported
 */
class UnsupportedCurrencyException(currency: String): Exception("Unsupported currency: $currency") // TODO: Include in controller advice

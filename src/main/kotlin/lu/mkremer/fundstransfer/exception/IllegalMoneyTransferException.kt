package lu.mkremer.fundstransfer.exception

/**
 * This exception is thrown when attempting to perform a money transfer, where
 * framework conditions do not allow such transfer to take place.
 */
class IllegalMoneyTransferException(message: String): Exception(message)

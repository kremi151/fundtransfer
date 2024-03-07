package lu.mkremer.fundstransfer.exception

/**
 * An exception that is thrown when a service is being used that is not ready yet
 */
class ServiceNotReadyException(message: String): Exception(message)
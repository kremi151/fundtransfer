package lu.mkremer.fundstransfer.service

import lu.mkremer.fundstransfer.datamodel.exchanger.ExchangeRates

/**
 * An interface for a service responsible for fetching exchange rates from an
 * external service,
 */
interface ExchangeRateSynchronizer {

    /**
     * Fetches exchange rates from the external service
     */
    fun fetch(): ExchangeRates

}

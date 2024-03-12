package lu.mkremer.fundstransfer.service.impl

import lu.mkremer.fundstransfer.datamodel.exchanger.ExchangeRates
import lu.mkremer.fundstransfer.service.ExchangeRateSynchronizer
import org.springframework.stereotype.Service

/**
 * Dummy implementation of [ExchangeRateSynchronizer] which doesn't do anything.
 * This is required so that the autowiring of [ExchangeRateSynchronizer] works,
 * since we do not load [ExchangeRateSynchronizerFrankfurter] during tests.
 * During test execution, tests should mock this service using @MockBean
 */
@Service
class DummyExchangeRateSynchronizer: ExchangeRateSynchronizer {
    override fun fetch(): ExchangeRates {
        return ExchangeRates(mapOf())
    }
}

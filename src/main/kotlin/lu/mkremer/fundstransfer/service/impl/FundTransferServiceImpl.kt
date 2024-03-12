package lu.mkremer.fundstransfer.service.impl

import jakarta.annotation.PostConstruct
import lu.mkremer.fundstransfer.datamodel.dto.MonetaryAmountDTO
import lu.mkremer.fundstransfer.datamodel.exchanger.ExchangeRates
import lu.mkremer.fundstransfer.exception.ServiceNotReadyException
import lu.mkremer.fundstransfer.exception.UnsupportedCurrenciesException
import lu.mkremer.fundstransfer.service.ExchangeRateSynchronizer
import lu.mkremer.fundstransfer.service.FundTransferService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.lang.invoke.MethodHandles
import java.util.concurrent.CompletableFuture

@Service
class FundTransferServiceImpl @Autowired constructor(
    private val exchangeRateSynchronizer: ExchangeRateSynchronizer,
): FundTransferService {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }

    private var exchangeRates: ExchangeRates? = null

    @PostConstruct
    fun initialize() {
        LOGGER.info("Initializing exchangers...")
        try {
            exchangeRates = exchangeRateSynchronizer.fetch()
            LOGGER.info("Initialized exchangers")
        } catch (e: Exception) {
            LOGGER.error("Unable to initialize exchange rates", e)
        }
    }

    @Scheduled(
        initialDelayString = "\${app.fundtransfer.exchange_rates.refresh_interval}",
        fixedRateString = "\${app.fundtransfer.exchange_rates.refresh_interval}",
    )
    @Async
    override fun updateExchangeRates(): CompletableFuture<Unit> {
        return try {
            exchangeRates = exchangeRateSynchronizer.fetch()
            CompletableFuture.completedFuture(Unit)
        } catch (e: Exception) {
            // There was an issue retrieving the exchange rates, so it's best
            // to clear the existing ones to prevent that incoming transactions
            // make use of outdated values
            exchangeRates = null

            LOGGER.error("Unable to update exchange rates", e)
            CompletableFuture.failedFuture(e)
        }
    }

    override fun convert(monetaryAmount: MonetaryAmountDTO, targetCurrency: String): MonetaryAmountDTO {
        return exchangeRates?.let {
            if (it.supportsCurrency(monetaryAmount.currency) && it.supportsCurrency(targetCurrency)) {
                MonetaryAmountDTO(
                    amount = it.convert(monetaryAmount, targetCurrency),
                    currency = targetCurrency,
                )
            } else {
                val unsupportedCurrencies = listOf(monetaryAmount.currency, targetCurrency)
                    .filter { c -> !it.supportsCurrency(c) }
                    .toSet()
                throw UnsupportedCurrenciesException(unsupportedCurrencies)
            }
        } ?: throw ServiceNotReadyException("Exchange rates are not loaded yet")
    }

    override fun supportsCurrency(currency: String): Boolean = exchangeRates?.supportsCurrency(currency) == true

    override val ready: Boolean get() = exchangeRates != null

}
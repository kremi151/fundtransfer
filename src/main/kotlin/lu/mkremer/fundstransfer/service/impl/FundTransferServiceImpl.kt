package lu.mkremer.fundstransfer.service.impl

import jakarta.annotation.PostConstruct
import lu.mkremer.fundstransfer.datamodel.dto.MonetaryAmountDTO
import lu.mkremer.fundstransfer.exception.UnsupportedCurrencyException
import lu.mkremer.fundstransfer.service.CurrencyExchanger
import lu.mkremer.fundstransfer.service.FundTransferService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.CustomizableThreadFactory
import org.springframework.stereotype.Service
import java.lang.invoke.MethodHandles
import java.util.concurrent.Executors

@Service
class FundTransferServiceImpl @Autowired constructor(
    private val exchangers: List<CurrencyExchanger>,
): FundTransferService {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }

    private val updaterExecutor = Executors.newCachedThreadPool(
        CustomizableThreadFactory("exchange-update-")
    )

    @PostConstruct
    fun initialize() {
        LOGGER.info("Initializing exchangers...")
        exchangers.forEach { it.update() }
        LOGGER.info("Initialized exchangers")
    }

    @Scheduled(
        initialDelayString = "\${app.fundtransfer.exchange_rates.refresh_interval}",
        fixedRateString = "\${app.fundtransfer.exchange_rates.refresh_interval}",
    )
    fun updateExchangeRates() {
        exchangers.forEach {
            updaterExecutor.submit {
                // TODO: Immutability?
                it.update()
            }
        }
    }

    override fun convert(monetaryAmount: MonetaryAmountDTO, targetCurrency: String): MonetaryAmountDTO {
        // TODO: Could we improve this by caching which exchanger handles which currencies?
        exchangers.forEach {
            if (it.supportsCurrency(monetaryAmount.currency) && it.supportsCurrency(targetCurrency)) {
                return MonetaryAmountDTO(
                    amount = it.convert(monetaryAmount, targetCurrency),
                    currency = targetCurrency,
                )
            }
        }
        throw UnsupportedCurrencyException(targetCurrency) // TODO: What about source currency?
    }

}
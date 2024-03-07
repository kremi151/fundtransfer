package lu.mkremer.fundstransfer.service.impl

import jakarta.annotation.PostConstruct
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
                it.update()
            }
        }
    }

}
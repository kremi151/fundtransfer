package lu.mkremer.fundstransfer.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import lu.mkremer.fundstransfer.service.FundTransferService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.lang.invoke.MethodHandles

/**
 * A validator that verifies whether a given input is both:
 * - An uppercase string containing exactly 3 letters
 * - Represents a currency that is supported by the system
 */
class CurrencyValidator: ConstraintValidator<Currency, String> {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private val REGEX = "^[A-Z]{3}\$".toRegex()
    }

    @Autowired
    private lateinit var fundTransferService: FundTransferService

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null || !REGEX.matches(value)) {
            LOGGER.debug("Not a valid currency string: $value")
            return false
        }
        // If the service is not ready, we will not check here for support.
        // Instead, we rely on the service layer to throw a ServiceNotReadyException, or
        // UnsupportedCurrenciesException as a fallback if needed.
        if (fundTransferService.ready && !fundTransferService.supportsCurrency(value)) {
            LOGGER.debug("Unsupported currency: $value")
            return false
        }
        return true
    }

}
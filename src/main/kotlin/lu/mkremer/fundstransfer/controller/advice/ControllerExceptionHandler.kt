package lu.mkremer.fundstransfer.controller.advice

import lu.mkremer.fundstransfer.datamodel.dto.ValidationErrorDTO
import lu.mkremer.fundstransfer.exception.IllegalMoneyTransferException
import lu.mkremer.fundstransfer.exception.InsufficientBalanceException
import lu.mkremer.fundstransfer.exception.UnsupportedCurrenciesException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * An advice for REST API controllers that handles uncaught exceptions in order
 * to return an adequate response to the client
 */
@RestControllerAdvice
class ControllerExceptionHandler {

    /**
     * A handler that catches validation errors detected by Hibernate validator,
     * returning the map of fields to their respective error messages, together
     * with a 400 Bad Request status code
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ValidationErrorDTO> {
        return ResponseEntity
            .badRequest()
            .body(
                ValidationErrorDTO(
                    message = "At least one submitted property has an invalid value",
                    fieldErrors = e.fieldErrors.associate {
                        it.field to (it.defaultMessage ?: "Unknown error")
                    },
                )
            )
    }

    /**
     * A handler for missing items, returning a simple 404 Not Found response.
     * This is especially convenient when using [java.util.Optional.orElseThrow].
     */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(e: NoSuchElementException): ResponseEntity<Unit> {
        return ResponseEntity.notFound().build()
    }

    /**
     * A handler to deal with situations where more money is attempted to be withdrawn
     * or transferred from an account than actually available.
     * Returns a 400 Bad Request response with an error message.
     */
    @ExceptionHandler(InsufficientBalanceException::class)
    fun handleInsufficientBalanceException(e: InsufficientBalanceException): ResponseEntity<ValidationErrorDTO> {
        return ResponseEntity
            .badRequest()
            .body(
                ValidationErrorDTO(message = "Account ${e.accountId} has insufficient balance: ${e.missing} ${e.currency} are missing")
            )
    }

    /**
     * A handler to deal with money transfers that cannot be executed due to
     * failing preconditions.
     * Returns a 400 Bad Request response with an error message.
     */
    @ExceptionHandler(IllegalMoneyTransferException::class)
    fun handleIllegalMoneyTransferException(e: IllegalMoneyTransferException): ResponseEntity<ValidationErrorDTO> {
        return ResponseEntity
            .badRequest()
            .body(
                ValidationErrorDTO(message = "Transfer could not be performed: ${e.message}")
            )
    }

    /**
     * A handler to deal with unsupported currencies, in case the initial
     * input validation passed.
     * The error message is taken from [UnsupportedCurrenciesException]'s
     * message, which contains a list of unsupported currencies.
     * Returns a 400 Bad Request response with an error message.
     */
    @ExceptionHandler(UnsupportedCurrenciesException::class)
    fun handleUnsupportedCurrenciesException(e: UnsupportedCurrenciesException): ResponseEntity<ValidationErrorDTO> {
        return ResponseEntity
            .badRequest()
            .body(
                ValidationErrorDTO(message = e.message)
            )
    }
}
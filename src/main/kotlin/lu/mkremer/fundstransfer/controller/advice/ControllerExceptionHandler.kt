package lu.mkremer.fundstransfer.controller.advice

import lu.mkremer.fundstransfer.datamodel.dto.ValidationErrorDTO
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
                    errors = e.fieldErrors.associate {
                        it.field to (it.defaultMessage ?: "Unknown error")
                    },
                )
            )
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(e: NoSuchElementException): ResponseEntity<Unit> {
        return ResponseEntity.notFound().build()
    }
}
package lu.mkremer.fundstransfer.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.constraints.Pattern
import kotlin.reflect.KClass

/**
 * A custom validation annotation used to validate currency strings in user
 * inputs via the REST API.
 * The validation itself is automatically performed by Hibernate Validator.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Pattern(regexp = "^[A-Z]{3}$", message = "The currency must be 3 letters in uppercase format")
@Constraint(validatedBy = [])
annotation class Currency(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

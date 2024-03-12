package lu.mkremer.fundstransfer.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * A custom validation annotation used to validate currency strings in user
 * inputs via the REST API, expecting currencies to be 3 uppercase letters
 * big strings and to be supported by the system.
 * The validation itself is automatically performed by Hibernate Validator.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [CurrencyValidator::class])
annotation class Currency(
    val message: String = "The currency is not an uppercase char sequence of 3 letters, or not supported",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

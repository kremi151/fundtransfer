package lu.mkremer.fundstransfer.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.constraints.Pattern
import kotlin.reflect.KClass

/**
 * A custom validation annotation used to validate account IDs in user inputs
 * via the REST API.
 * The validation itself is automatically performed by Hibernate Validator.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Pattern(regexp = "^[0-9]{9}$", message = "The account ID must be a 9-digit number (with leading zeros)")
@Constraint(validatedBy = [])
annotation class AccountId(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

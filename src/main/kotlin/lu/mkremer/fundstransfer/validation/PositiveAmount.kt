package lu.mkremer.fundstransfer.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.constraints.DecimalMin
import kotlin.reflect.KClass

/**
 * A custom validation annotation used to validate monetary amounts in user
 * inputs via the REST API, expecting amounts to be strictly positive.
 * The validation itself is automatically performed by Hibernate Validator.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@DecimalMin(value = "0.0", inclusive = false, message = "The amount must be strictly positive")
@Constraint(validatedBy = [])
annotation class PositiveAmount(
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

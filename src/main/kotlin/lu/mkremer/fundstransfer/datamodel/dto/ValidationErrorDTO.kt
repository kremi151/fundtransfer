package lu.mkremer.fundstransfer.datamodel.dto

/**
 * A simple DTO returned by REST endpoints in case of a validation error
 */
data class ValidationErrorDTO(
    /**
     * A map from property names to a human readable error message, describing
     * the validation error
     */
    val errors: Map<String, String>,
)

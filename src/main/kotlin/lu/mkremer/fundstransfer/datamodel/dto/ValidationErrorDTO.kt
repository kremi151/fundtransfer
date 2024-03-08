package lu.mkremer.fundstransfer.datamodel.dto

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * A simple DTO returned by REST endpoints in case of a validation error
 */
data class ValidationErrorDTO(

    /**
     * A human readable error message describing the error
     */
    val message: String,

    /**
     * A map from property names to a human readable error message, describing
     * the validation error
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val fieldErrors: Map<String, String>? = null,
)

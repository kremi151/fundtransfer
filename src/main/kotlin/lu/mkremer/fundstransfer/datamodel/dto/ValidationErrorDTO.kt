package lu.mkremer.fundstransfer.datamodel.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A description of a user input error")
data class ValidationErrorDTO(

    @Schema(
        description = "A human readable error message describing the error",
        required = true,
    )
    val message: String,

    @Schema(
        description = "A map from property names to a human readable error message, describing the validation error",
        required = false,
    )
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val fieldErrors: Map<String, String>? = null,
)

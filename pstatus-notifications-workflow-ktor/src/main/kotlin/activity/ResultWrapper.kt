package gov.cdc.ocio.processingnotifications.activity

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable


/**
 * A sealed class representing the outcome of an operation, either a success or failure.
 *
 * @param T The type of the successful result, which must implement the [DataResponse] interface.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "resultType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ResultWrapper.Success::class, name = "Success"),
    JsonSubTypes.Type(value = ResultWrapper.Failure::class, name = "Failure")
)
sealed class ResultWrapper<out T : DataResponse> : Serializable {

    /**
     * Represents a successful result of an operation that wraps a value of type [T].
     *
     * @param T The type of the success value, constrained to classes implementing the [DataResponse] interface.
     * @property value The successful result of the operation.
     */
    data class Success<out T : DataResponse>(
        @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type"
        )
        val value: T
    ) : ResultWrapper<T>()

    /**
     * Represents a failure result of an operation. This class is a specific implementation of [ResultWrapper]
     * that encapsulates error details.
     *
     * @property errorMessage A descriptive message providing details about the failure.
     * @property errorType An optional string representing the type or category of the error.
     */
    data class Failure(
        val errorMessage: String,
        val errorType: String? = null
    ) : ResultWrapper<Nothing>()

    /**
     * Returns the value if this is [Success], otherwise throws an exception.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw RuntimeException(
            "ResultWrapper Failure: $errorMessage" +
                    (errorType?.let { " (type: $errorType)" } ?: "")
        )
    }
}
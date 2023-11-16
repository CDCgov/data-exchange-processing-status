package gov.cdc.ocio.processingstatusapi.model

import com.google.gson.annotations.SerializedName

/**
 * Request type for service bus messages, which can be to either create or amend an existing report.
 */
enum class RequestType {

    UNDEFINED,

    @SerializedName("create")
    CREATE,

    @SerializedName("amend")
    AMEND
}

/**
 * Disposition type specifies whether an amended report replaces an existing one
 * or adds a new one.  If *replace* is specified but none exists then it will
 * be added.  The *replace* disposition type is useful when a stage wants to provide
 * frequent updates, but doesn't need or want to keep a history of those updates.
 * An example of this is the upload stage when a file upload is in progress.
 */
enum class DispositionType {

    @SerializedName("add")
    APPEND,

    @SerializedName("replace")
    REPLACE
}

/**
 * Base class for all service bus messages.  Contains all the common required parameters for all service bus messages.
 * Note the ServiceBusMessage class must be *open* not *abstract* as it will need to be initially created to determine
 * the type.
 *
 * @property requestType RequestType
 * @property dispositionType DispositionType
 */
open class ServiceBusMessage {

    var requestType = RequestType.UNDEFINED

    // Default is to append
    var dispositionType = DispositionType.APPEND
}
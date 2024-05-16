package gov.cdc.ocio.processingstatusapi.models

import com.google.gson.annotations.SerializedName

/**
 * Disposition type specifies whether an amended report replaces an existing one
 * or adds a new one.  If *replace* is specified but none exists then it will
 * be added.  The *replace* disposition type is useful when a stage wants to provide
 * frequent updates, but doesn't need or want to keep a history of those updates.
 * An example of this is the upload stage when a file upload is in progress.
 */
enum class DispositionType {

    @SerializedName("add")
    ADD,

    @SerializedName("replace")
    REPLACE
}

/**
 * Base class for all service bus messages.  Contains all the common required parameters for all service bus messages.
 * Note the ServiceBusMessage class must be *open* not *abstract* as it will need to be initially created to determine
 * the type.
 *
 * @property dispositionType DispositionType
 */
open class ServiceBusMessage {

    @SerializedName("disposition_type")
    // Default is to add
    var dispositionType = DispositionType.ADD
}
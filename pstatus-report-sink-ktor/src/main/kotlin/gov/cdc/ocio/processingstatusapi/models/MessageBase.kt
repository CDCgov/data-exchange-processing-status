package gov.cdc.ocio.processingstatusapi.models

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.messagesystem.models.DispositionType

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
 * Base class for all messages, supporting various messaging systems such as Azure Service Bus, AWS SQS and RabbitMQ.
 * This class contains common required parameters for handling messages across these systems.
 *
 * Note that the `MessageBase` class must be *open* not *abstract* as it may need to be instantiated to determine
 * the type of message at runtime.
 *
 * @property dispositionType DispositionType The action or state of the message, defaulting to ADD.
 */
open class MessageBase {

    @SerializedName("disposition_type")
    // Default is to add
    var dispositionType = DispositionType.ADD
}
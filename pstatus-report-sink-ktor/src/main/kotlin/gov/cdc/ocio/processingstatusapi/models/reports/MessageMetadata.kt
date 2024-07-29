package gov.cdc.ocio.processingstatusapi.models.reports

import com.google.gson.annotations.SerializedName
import gov.cdc.ocio.processingstatusapi.models.ServiceBusMessage


/**
 * Get MessageMetadata.
 *
 * @property provenance Provenance?
 *
 */
class MessageMetadata: ServiceBusMessage() {

    @SerializedName("provenance")
    var provenance: Provenance?=null

}
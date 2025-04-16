package gov.cdc.ocio.processingnotifications.model

import gov.cdc.ocio.types.notification.Notifiable

data class DeadlineCheck(val dataStreamId: String, val jurisdiction: String, val timestamp: String) : Notifiable {
    override fun subject(): String {
        return "PHDO UPLOAD DEADLINE CHECK EXPIRED for $jurisdiction on $timestamp"
    }

    override fun buildEmailBody(): String {
        return "Upload deadline over.  Failed to get upload for dataStreamId: $dataStreamId}, jurisdiction: $jurisdiction, at $timestamp"
    }

    override fun buildWebhookBody(): Any {
        TODO("Not yet implemented")
    }

}
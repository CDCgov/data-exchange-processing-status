package gov.cdc.ocio.processingstatusnotifications.model

interface NotificationAction {
    fun doNotify(payload: Any)
}
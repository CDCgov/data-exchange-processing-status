package gov.cdc.ocio.types.model

interface Notifiable {
    fun subject(): String // probably a better way to get the subject
    fun buildEmailBody(): String
    fun buildWebhookBody(): Any // TODO make webhook envelope data class
}
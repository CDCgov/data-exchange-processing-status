package gov.cdc.ocio.notificationdispatchers.model

enum class EmailDispatcherType(val value: String) {
    SMTP("smtp"),
    LOGGER("logger")
}
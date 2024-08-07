package gov.cdc.ocio.processingstatusnotifications.EasyRules

data class Rule(
    val name: String,
    val description: String,
    val priority: Int,
    val condition: String,
    val actions: List<String>
)


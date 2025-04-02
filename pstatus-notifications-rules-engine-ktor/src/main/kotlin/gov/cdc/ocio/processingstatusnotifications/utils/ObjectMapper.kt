package gov.cdc.ocio.processingstatusnotifications.utils

object ObjectMapper {

    /**
     * Maps an object of [Any] type to a [Map]<[String], [Any]?>.
     * @param any [Any]
     * @return [Map]<[String], [Any]?>
     */
    fun anyToMap(any: Any): Map<String, Any?> {
        return any::class.java.declaredFields.associate { field ->
            field.isAccessible = true
            field.name to field.get(any)
        }
    }
}
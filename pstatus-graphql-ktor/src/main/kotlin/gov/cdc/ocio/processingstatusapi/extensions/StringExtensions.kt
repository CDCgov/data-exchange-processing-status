package gov.cdc.ocio.processingstatusapi.extensions

import com.fasterxml.jackson.databind.PropertyNamingStrategies

fun String.snakeToCamelCase(): String {
    return split("_").joinToString("") { it.capitalize() }
}

//fun String.toSnakeCase(): String {
//    return this.replace(Regex("([a-z])([A-Z])")) {
//        "${it.groupValues[1]}_${it.groupValues[2].lowercase()}"
//    }.lowercase()
//}

fun String.camelToSnakeCase(): String {
    val sb = StringBuilder()
    for (char in this) {
        if (char.isUpperCase()) {
            sb.append('_').append(char.lowercaseChar())
        } else {
            sb.append(char)
        }
    }
    return sb.toString()
}
package gov.cdc.ocio.processingstatusapi.extensions


fun String.snakeToCamelCase(): String {
    val words = split("_")
    return buildString {
        append(words[0].lowercase())
        for (i in 1 until words.size) {
            append(words[i].replaceFirstChar { it.uppercase() })
        }
    }
}

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
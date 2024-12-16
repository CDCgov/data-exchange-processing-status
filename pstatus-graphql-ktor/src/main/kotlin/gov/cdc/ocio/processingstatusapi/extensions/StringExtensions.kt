package gov.cdc.ocio.processingstatusapi.extensions


/**
 * Convert this string from snake case to camel case, where the first word is not capitalized.
 *
 * @receiver [String] - this string, which is assumed to be a snake case string.
 * @return [String] - this string converted to camel case.
 */
fun String.snakeToCamelCase(): String {
    val words = split("_")
    return buildString {
        append(words[0].lowercase())
        for (i in 1 until words.size) {
            append(words[i].replaceFirstChar { it.uppercase() })
        }
    }
}

/**
 * Convert this string from camel case to snake case.
 *
 * @receiver [String] - this string, which is assumed to be a camel case string.
 * @return [String] - this string converted to snake case.
 */
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
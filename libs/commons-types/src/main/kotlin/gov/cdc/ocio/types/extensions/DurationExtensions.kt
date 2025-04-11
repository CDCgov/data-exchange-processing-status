package gov.cdc.ocio.types.extensions

import java.time.Duration


/**
 * Extension to convert a [Duration] to a human-readable string
 *
 * @receiver Duration
 * @return String
 */
fun Duration.toHumanReadable(): String {
    val milliseconds = this.toMillis()
    val seconds = milliseconds / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    val remainingMilliseconds = milliseconds % 1000
    val roundedMillis = Math.round(remainingMilliseconds / 100.0) / 10 // Round to tenths place

    return buildString {
        if (hours > 0) append("$hours hr ")
        if (minutes > 0) append("$minutes min ")
        append("$remainingSeconds${if (roundedMillis > 0) ".${(roundedMillis * 10).toInt()}" else ""} sec")
    }.trim()
}
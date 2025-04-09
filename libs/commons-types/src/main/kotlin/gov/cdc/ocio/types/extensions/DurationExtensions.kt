package gov.cdc.ocio.types.extensions

import java.time.Duration


/**
 * Extension to convert a [Duration] to a human-readable string
 *
 * @receiver Duration
 * @return String
 */
fun Duration.toHumanReadable(): String {
    val seconds = this.seconds
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60

    return buildString {
        if (hours > 0) append("$hours hour${if (hours > 1) "s" else ""} ")
        if (minutes > 0) append("$minutes minute${if (minutes > 1) "s" else ""} ")
        if (remainingSeconds > 0 || (hours == 0L && minutes == 0L)) append("$remainingSeconds second${if (remainingSeconds > 1) "s" else ""}")
    }.trim()
}
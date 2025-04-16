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

    return buildString {
        if (hours > 0) append("$hours hr ")
        if (minutes > 0) append("$minutes min ")
        if (remainingSeconds > 0 || remainingMilliseconds > 0 || (hours == 0L && minutes == 0L))
            append("%d.%03d sec".format(remainingSeconds, remainingMilliseconds))
    }.trim()
}
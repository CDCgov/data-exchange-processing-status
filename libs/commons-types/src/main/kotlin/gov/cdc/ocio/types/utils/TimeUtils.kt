package gov.cdc.ocio.types.utils

import org.ocpsoft.prettytime.PrettyTime
import java.util.*

object TimeUtils {

    /**
     * Format the time in milliseconds to 00:00:00.000 format.
     *
     * @param millis Long
     * @return String
     */
    fun formatMillisToHMS(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        val remainingMillis = millis % 1000

        return "%02d:%02d:%02d.%03d".format(hours, minutes, remainingSeconds, remainingMillis / 10)
    }

    /**
     * Formats a given time duration, specified in seconds, into a human-readable relative time string.
     * The method uses the PrettyTime library to generate a textual representation of the elapsed time
     * in relation to the current moment.
     *
     * @param secondsAgo The time duration in seconds that has passed from the current time.
     * @return A formatted string representing the relative time elapsed.
     */
    fun formatRelativeTime(secondsAgo: Long?): String {
        if (secondsAgo == null) return ""
        val past = Date(System.currentTimeMillis() - secondsAgo * 1000)
        val prettyTime = PrettyTime()
        return prettyTime.format(past)
    }
}
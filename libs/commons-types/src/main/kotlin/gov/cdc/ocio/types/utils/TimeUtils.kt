package gov.cdc.ocio.types.utils

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
}
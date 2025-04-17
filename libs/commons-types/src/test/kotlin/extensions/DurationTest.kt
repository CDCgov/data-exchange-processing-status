package extensions

import gov.cdc.ocio.types.extensions.toHumanReadable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class DurationTest {

    @Test
    fun `duration 2400 millis`() {
        val duration = Duration.ofMillis(2400)

        // Validate content
        assertEquals("2.400 sec", duration.toHumanReadable(), )
    }

    @Test
    fun `duration hours, minutes, and seconds`() {
        val duration = Duration.ofHours(2)
            .plusMinutes(32)
            .plusSeconds(26)
            .plusMillis(5)

        // Validate content
        assertEquals("2 hr 32 min 26.005 sec", duration.toHumanReadable())
    }

    @Test
    fun `duration hours only`() {
        val duration = Duration.ofHours(5)

        // Validate content
        assertEquals("5 hr", duration.toHumanReadable())
    }

    @Test
    fun `duration hours and minutes`() {
        val duration = Duration.ofHours(3)
            .plusMinutes(2)

        // Validate content
        assertEquals("3 hr 2 min", duration.toHumanReadable())
    }

    @Test
    fun `duration is zero`() {
        val duration = Duration.ofHours(0)

        // Validate content
        assertEquals("0.000 sec", duration.toHumanReadable())
    }
}
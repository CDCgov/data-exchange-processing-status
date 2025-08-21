package gov.cdc.ocio.types

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset


/**
 * Used to specify a date range as instants.
 *
 * @property start Instant
 * @property endInclusive Instant
 * @constructor
 */
class InstantRange(override val start: Instant, override val endInclusive: Instant)
    : ClosedRange<Instant> {

    companion object {

        /**
         * Creates a [InstantRange] from the provided [LocalDate], which will be assumed to be in the UTC timezone.
         *
         * @param date LocalDate
         * @return InstantRange
         */
        fun fromLocalDate(date: LocalDate): InstantRange {
            val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant() // 00:00:00 UTC
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() // 00:00:00 of next day UTC (exclusive)

            return InstantRange(startOfDay, endOfDay)
        }
    }
}
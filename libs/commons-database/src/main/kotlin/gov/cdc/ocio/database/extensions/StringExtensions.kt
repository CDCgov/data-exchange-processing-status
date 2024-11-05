package gov.cdc.ocio.database.extensions


/**
 * String extension to convert itself to a number if possible.
 *
 * @return Number?
 */
fun String.convertToNumber(): Number? {
    return toDoubleOrNull()?.let {
        if (it % 1 == 0.0) it.toLong() else it.toFloat()
    }
}
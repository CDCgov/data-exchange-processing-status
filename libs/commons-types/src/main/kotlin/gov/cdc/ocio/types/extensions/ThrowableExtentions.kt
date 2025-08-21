package gov.cdc.ocio.types.extensions

/**
 * Retrieves the root cause of this throwable by traversing the chain of causes.
 *
 * This function iterates through the cause chain of the current throwable instance
 * until it reaches the root cause, which is the last non-null cause in the chain.
 * A check is included to avoid issues with circular references in the cause chain.
 *
 * @return [Throwable] The root cause of this throwable, or this throwable itself if
 * it has no cause.
 */
fun Throwable.getRootCause(): Throwable {
    var rootCause: Throwable = this
    while (rootCause.cause != null && rootCause.cause !== rootCause) { // Added a check for self-referential causes
        rootCause = rootCause.cause!!
    }
    return rootCause
}
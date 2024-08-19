package gov.cdc.ocio.processingstatusapi.utils

import gov.cdc.ocio.processingstatusapi.models.query.UploadsStatus

/**
 * Utility class for sorting operations on UploadsStatus.
 */
object SortUtils {

    /**
     * Sorts the items in UploadsStatus based on the specified field and order.
     *
     * @param uploadsStatus The UploadsStatus instance to be sorted.
     * @param field The field name to sort by.
     * @param ascending If true, sorts in ascending order; if false, sorts in descending order.
     */
    fun sortByField(uploadsStatus: UploadsStatus, field: String, order: String) {
        when (field) {
            "fileName" -> {
                when (order.lowercase()) {
                    "asc" -> uploadsStatus.items.sortBy { it.fileName }
                    "desc" -> uploadsStatus.items.sortByDescending { it.fileName }
                    else -> throw IllegalArgumentException("Unsupported order: $order")
                }
            }
            "date" -> {
                when (order.lowercase()) {
                    "asc" -> uploadsStatus.items.sortBy { it.timestamp }
                    "desc" -> uploadsStatus.items.sortByDescending { it.timestamp }
                    else -> throw IllegalArgumentException("Unsupported order: $order")
                }
            }
            else -> throw IllegalArgumentException("Unsupported field: $field")
        }
    }
}
package gov.cdc.ocio.processingstatusapi.utils

import gov.cdc.ocio.processingstatusapi.models.query.UploadsStatus

/**
 * Utility class for sorting operations on UploadsStatus.
 */
object SortUtils {

    private enum class SortOrder { ASC, DESC }

    /**
     * Sorts the items in UploadsStatus based on the specified field and order.
     *
     * @param uploadsStatus The UploadsStatus instance to be sorted.
     * @param field The field name to sort by.
     * @param order The order to sort in ("asc" or "desc").
     */
    fun sortByField(uploadsStatus: UploadsStatus, field: String, order: String) {
        val sortOrder = try {
            SortOrder.valueOf(order.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Unsupported order: $order")
        }

        when (field) {
            "date" -> {
                if (sortOrder == SortOrder.ASC) {
                    uploadsStatus.items.sortBy { it.timestamp }
                } else {
                    uploadsStatus.items.sortByDescending { it.timestamp }
                }
            }
            "fileName" -> {
                if (sortOrder == SortOrder.ASC) {
                    uploadsStatus.items.sortBy { it.fileName }
                } else {
                    uploadsStatus.items.sortByDescending { it.fileName }
                }
            }
            "dataStreamId" -> {
                if (sortOrder == SortOrder.ASC) {
                    uploadsStatus.items.sortBy {
                        (it.metadata?.get("data_stream_id") as? String)?.lowercase() ?: ""
                    }
                } else {
                    uploadsStatus.items.sortByDescending {
                        (it.metadata?.get("data_stream_id") as? String)?.lowercase() ?: ""
                    }
                }
            }
            "dataStreamRoute" -> {
                if (sortOrder == SortOrder.ASC) {
                    uploadsStatus.items.sortBy {
                        (it.metadata?.get("data_stream_route") as? String)?.lowercase() ?: ""
                    }
                } else {
                    uploadsStatus.items.sortByDescending {
                        (it.metadata?.get("data_stream_route") as? String)?.lowercase() ?: ""
                    }
                }
            }
            "status" -> {
                if (sortOrder == SortOrder.ASC) {
                    uploadsStatus.items.sortBy { it.status }
                } else {
                    uploadsStatus.items.sortByDescending { it.status }
                }
            }
            "jurisdiction" -> {
                if (sortOrder == SortOrder.ASC) {
                    uploadsStatus.items.sortBy { it.jurisdiction }
                } else {
                    uploadsStatus.items.sortByDescending { it.jurisdiction }
                }
            }
            "senderId" -> {
                if (sortOrder == SortOrder.ASC) {
                    uploadsStatus.items.sortBy { it.senderId }
                } else {
                    uploadsStatus.items.sortByDescending { it.senderId }
                }
            }
            else -> uploadsStatus.items.sortByDescending { it.timestamp }
        }


        //Sort the senderIds and jurisdiction fields in the PageSummary
        uploadsStatus.summary.senderIds.sort()
        uploadsStatus.summary.jurisdictions.sort()

    }
}

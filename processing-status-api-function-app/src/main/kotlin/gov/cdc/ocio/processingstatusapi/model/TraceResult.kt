package gov.cdc.ocio.processingstatusapi.model

class TraceResult {

    var status: String? = null

    companion object {

//        fun createFromItem(/*item: Item*/): TraceResult {
//            var percentComplete = 0F
//            if (item.size > 0)
//                percentComplete = item.offset.toFloat() / item.size * 100
//
//            val statusMessage = if (item.offset < item.size) "Uploading" else "Complete"
//            return TraceResult().apply {
//                status = statusMessage
//                tus_upload_id = item.tguid
//                file_name = item.filename
//                file_size_bytes = item.size
//                bytes_uploaded = item.offset
//                percent_complete = percentComplete
//                time_uploading_sec = item.end_time_epoch - item.start_time_epoch
//                metadata = item.metadata
//                timestamp = item.getTimestamp()
//            }
//        }
    }
}
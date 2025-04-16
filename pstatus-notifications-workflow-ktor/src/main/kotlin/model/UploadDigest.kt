package gov.cdc.ocio.processingnotifications.model

data class UploadDigest(val counts: Map<String, Map<String, Map<String, Int>>>, val timestamp: String)
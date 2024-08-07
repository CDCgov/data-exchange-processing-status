import java.time.LocalTime

data class Jurisdiction(val name: String, val lastUploadTime: LocalTime?)
data class UploadStatus(val completed: Boolean, val hasErrors: Boolean)
data class DataStream(val name: String, val uploadCount: Int)

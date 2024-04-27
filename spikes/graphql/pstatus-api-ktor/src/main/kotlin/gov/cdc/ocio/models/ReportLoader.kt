package gov.cdc.ocio.models

import com.azure.cosmos.models.CosmosQueryRequestOptions
import gov.cdc.ocio.cosmos.CosmosRepository
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReportLoader: KoinComponent {

    private val cosmosRepository by inject<CosmosRepository>()

    private val reportsContainerName = "Reports"

    private val reportsContainer = cosmosRepository.reportsContainer

    private val logger = KotlinLogging.logger {}

    fun getByUploadId(uploadId: String): Report? {
        val reportsSqlQuery = "select * from $reportsContainerName r where r.uploadId = '$uploadId'"

        val reportItems = reportsContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            Report::class.java
        )

        return reportItems.firstOrNull()
    }

    fun search(ids: List<String>): List<Report> {
        val reportsSqlQuery = "select * from $reportsContainerName r where r.id = '${ids.first()}'"

        val reportItems = reportsContainer.queryItems(
            reportsSqlQuery, CosmosQueryRequestOptions(),
            Report::class.java
        )

        return reportItems.toList()
    }

}

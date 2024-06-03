package gov.cdc.ocio.processingstatusapi.loaders

import gov.cdc.ocio.processingstatusapi.cosmos.CosmosRepository
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class CosmosLoader: KoinComponent {

    private val cosmosRepository by inject<CosmosRepository>()

    protected val reportsContainerName = "Reports"

    protected val reportsContainer = cosmosRepository.reportsContainer

    protected val logger = KotlinLogging.logger {}
}
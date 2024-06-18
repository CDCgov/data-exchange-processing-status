package gov.cdc.ocio.processingstatusapi.loaders

import gov.cdc.ocio.processingstatusapi.cosmos.CosmosDeadLetterRepository
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class CosmosDeadLetterLoader: KoinComponent {

    private val cosmosRepository by inject<CosmosDeadLetterRepository>()

    protected val reportsDeadLetterContainerName = "ReportsDeadLetter"

    protected val reportsDeadLetterContainer = cosmosRepository.reportsDeadLetterContainer

    protected val logger = KotlinLogging.logger {}
}
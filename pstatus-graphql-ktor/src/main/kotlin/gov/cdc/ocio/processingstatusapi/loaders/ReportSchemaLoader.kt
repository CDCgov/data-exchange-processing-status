package gov.cdc.ocio.processingstatusapi.loaders

import gov.cdc.ocio.processingstatusapi.models.schemas.SchemaLoaderInfoGql
import gov.cdc.ocio.processingstatusapi.models.schemas.ReportSchemaMetadataGql
import gov.cdc.ocio.reportschemavalidator.utils.CloudSchemaLoaderConfiguration
import io.ktor.server.application.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


/**
 * Report schema loader for facilitating GraphQL queries.
 *
 * @property environment ApplicationEnvironment
 * @property schemaLoader CloudSchemaLoader
 */
class ReportSchemaLoader : KoinComponent {

    private val environment by inject<ApplicationEnvironment>()

    private val schemaLoader = CloudSchemaLoaderConfiguration(environment).createSchemaLoader()

    /**
     * Provides the schema loader system information.
     *
     * @return SchemaLoaderInfoGql
     */
    fun loaderInfo() = SchemaLoaderInfoGql.from(schemaLoader.getInfo())

    /**
     * Provides a list of the available report schemas.
     *
     * @return List<[ReportSchemaMetadataGql]>
     */
    fun list() = schemaLoader.getSchemaFiles().map { ReportSchemaMetadataGql.from(it) }
}
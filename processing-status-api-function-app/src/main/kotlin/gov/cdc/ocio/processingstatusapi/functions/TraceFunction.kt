package gov.cdc.ocio.processingstatusapi.functions

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.ocio.processingstatusapi.model.TraceResult
//import gov.cdc.ocio.processingstatusapi.cosmos.CosmosClientManager
import java.util.*


class TraceFunction {

    fun run(
        request: HttpRequestMessage<Optional<String>>,
        providerName: String,
        context: ExecutionContext
    ): HttpResponseMessage {

        val logger = context.logger

        logger.info("HTTP trigger processed a ${request.httpMethod.name} request.")
        logger.info("provider name = $providerName")

//        val provider = request.queryParameters["provider"]

        val result = TraceResult()
        result.status = "OK"

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }


}
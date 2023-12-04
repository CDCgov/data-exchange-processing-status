package gov.cdc.ocio.processingstatusapi.functions

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import java.util.*

class HealthCheckFunction {

    fun run(
        request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext
    ): HttpResponseMessage {
    
        // todo

        return request
            .createResponseBuilder(HttpStatus.OK)
            .build()
    }
}
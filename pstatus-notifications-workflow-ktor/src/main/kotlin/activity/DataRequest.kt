package gov.cdc.ocio.processingnotifications.activity

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.DeadlineCheckRequest
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsRequest
import gov.cdc.ocio.processingnotifications.workflow.toperrors.TopErrorsRequest
import java.io.Serializable

/**
 * Interface representing a request for data processing activities.
 *
 * This interface serves as a marker for various types of data requests related to
 * notification processing workflows. Implementing classes must specify the concrete
 * details of the request. It supports polymorphic deserialization using Jackson annotations.
 *
 * The `type` property is used to differentiate between different subtypes of `DataRequest`
 * during deserialization, enabling extensibility for additional request types.
 *
 * Implementations of this interface, such as `DeadlineCheckRequest`, provide specific details
 * required for their respective operations.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = DeadlineCheckRequest::class),
    JsonSubTypes.Type(value = TopErrorsRequest::class),
    JsonSubTypes.Type(value = UploadDigestCountsRequest::class)
)
interface DataRequest : Serializable
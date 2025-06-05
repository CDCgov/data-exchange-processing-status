package gov.cdc.ocio.processingnotifications.activity

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import gov.cdc.ocio.processingnotifications.workflow.deadlinecheck.DeadlineCheckResponse
import gov.cdc.ocio.processingnotifications.workflow.digestcounts.UploadDigestCountsResponse
import gov.cdc.ocio.processingnotifications.workflow.toperrors.TopErrorsResponse
import java.io.Serializable


/**
 * Marker interface representing a generic data response for processing activities.
 *
 * This interface is designed to support polymorphic deserialization and acts
 * as a common contract for various types of data responses within the system.
 * Implementing classes are expected to provide specific details and structure
 * relevant to their use cases.
 *
 * The `type` property, specified in the annotation, is used to differentiate
 * between different subtypes of `DataResponse` during deserialization, ensuring
 * extensibility for additional response types.
 *
 * Subtypes of this interface, such as `DeadlineCheckResponse`, implement domain-specific
 * logic and carry the respective data relevant to that particular operation.
 *
 * This interface extends [Serializable] to facilitate object serialization and
 * deserialization in distributed applications or for storage purposes.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = DeadlineCheckResponse::class),
    JsonSubTypes.Type(value = TopErrorsResponse::class),
    JsonSubTypes.Type(value = UploadDigestCountsResponse::class)
)
interface DataResponse : Serializable
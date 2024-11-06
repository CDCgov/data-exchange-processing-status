package gov.cdc.ocio.database.dynamo

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.time.Instant

/**
 * Attribute converter for Instants using epoch as the base type (long)
 */
class InstantToEpochConverter : AttributeConverter<Instant> {

    /**
     * Implementation of the transform from instant to attribute type.
     *
     * @param instant Instant
     * @return AttributeValue
     */
    override fun transformFrom(instant: Instant): AttributeValue {
        return AttributeValue.builder()
            .n(instant.toEpochMilli().toString())
            .build()
    }

    /**
     * Implementation of the transform to an instant from an attribute value of type long or string.
     *
     * @param attributeValue AttributeValue
     * @return Instant?
     */
    override fun transformTo(attributeValue: AttributeValue): Instant? {
        return when (attributeValue.type()) {
            AttributeValue.Type.N -> Instant.ofEpochMilli(attributeValue.n().toLong())
            AttributeValue.Type.S -> Instant.parse(attributeValue.s())
            else -> null
        }
    }

    /**
     * Type of the attribute, which is Instant
     *
     * @return EnhancedType<Instant>
     */
    override fun type(): EnhancedType<Instant> {
        return EnhancedType.of(Instant::class.java)
    }

    /**
     * Attribute value type of the converter, which is N or Number.
     *
     * @return AttributeValueType
     */
    override fun attributeValueType(): AttributeValueType {
        return AttributeValueType.N
    }
}
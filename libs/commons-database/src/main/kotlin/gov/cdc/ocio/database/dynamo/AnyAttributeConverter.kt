package gov.cdc.ocio.database.dynamo

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.JsonItemAttributeConverter
import software.amazon.awssdk.protocols.jsoncore.JsonNode
import software.amazon.awssdk.protocols.jsoncore.internal.NullJsonNode
import software.amazon.awssdk.services.dynamodb.model.AttributeValue


/**
 * Attribute converter for the Any type, which for dynamodb this can only be interpreted as a JsonNode.
 */
class AnyAttributeConverter : AttributeConverter<Any> {

    /**
     * Attempt to map to the JsonItemAttributeConverter if the input is a JsonNode.
     *
     * @param input Any
     * @return AttributeValue
     */
    override fun transformFrom(input: Any): AttributeValue {
        if (input is JsonNode) {
            return JsonItemAttributeConverter.create().transformFrom(input)
        }
        throw Exception("Unable to transform from Any object input type to a JsonNode")
    }

    /**
     * Attempt to transform the input to a JsonNode.
     *
     * @param input AttributeValue
     * @return Any
     */
    override fun transformTo(input: AttributeValue?): Any {
        if (AttributeValue.fromNul(true) == input) {
            return NullJsonNode.instance()
        }
        return JsonItemAttributeConverter.create().transformTo(input)
    }

    /**
     * The enhanced type is Any.
     *
     * @return EnhancedType<Any>
     */
    override fun type(): EnhancedType<Any> {
        return EnhancedType.of(Any::class.java)
    }

    /**
     * The attribute type value is M for map.
     *
     * @return AttributeValueType
     */
    override fun attributeValueType(): AttributeValueType {
        return AttributeValueType.M
    }

}
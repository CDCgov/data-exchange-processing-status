package gov.cdc.ocio.database.dynamo

import software.amazon.awssdk.enhanced.dynamodb.*
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.JsonItemAttributeConverter
import java.util.stream.Collectors


/**
 * Attribute converter provider for JsonNode attributes.  Note, this is only used by dynamodb.
 *
 * @property customConverters List<AttributeConverter<*>>
 * @property customConvertersMap MutableMap<EnhancedType<*>, AttributeConverter<*>>
 * @property defaultProvider AttributeConverterProvider
 */
class JsonNodeConverterProvider : AttributeConverterProvider {

    private val customConverters = listOf<AttributeConverter<*>>(
        JsonItemAttributeConverter.create()
    )

    private val customConvertersMap =
        customConverters.stream().collect(
            Collectors.toMap(
                { obj: AttributeConverter<*> -> obj.type() },
                { c: AttributeConverter<*> -> c }
            ))

    private val defaultProvider = DefaultAttributeConverterProvider.create()

    /**
     * Converter for the enhanced type generic.
     *
     * @param enhancedType EnhancedType<T>
     * @return AttributeConverter<T>
     */
    override fun <T> converterFor(enhancedType: EnhancedType<T>): AttributeConverter<T> {
        @Suppress("UNCHECKED_CAST")
        return customConvertersMap.computeIfAbsent(
            enhancedType
        ) { enhancedTypeEval: EnhancedType<*>? ->
            defaultProvider.converterFor(
                enhancedTypeEval
            )
        } as AttributeConverter<T>
    }
}

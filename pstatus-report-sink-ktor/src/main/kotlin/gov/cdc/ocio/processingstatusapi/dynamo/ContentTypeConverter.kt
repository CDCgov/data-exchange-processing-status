package gov.cdc.ocio.processingstatusapi.dynamo

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.awssdk.enhanced.dynamodb.*
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.util.stream.Collectors


abstract class Content : Map<String, Any>

class JsonContentConverter : AttributeConverter<Content?> {
    override fun transformFrom(input: Content?): AttributeValue {
        try {
            return AttributeValue.builder().s(objectMapper.writeValueAsString(input)).build()
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Unable to serialize Content to JSON", e)
        }
    }

    override fun transformTo(input: AttributeValue): Content? {
        try {
            return objectMapper.readValue(input.s(), Content::class.java)
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Unable to deserialize JSON to Content", e)
        }
    }

    override fun type(): EnhancedType<Content?> {
        return EnhancedType.of(Content::class.java)
    }

    override fun attributeValueType(): AttributeValueType {
        return AttributeValueType.S
    }

    companion object {
        private val objectMapper = ObjectMapper()
    }
}

class ContentConverterProvider : AttributeConverterProvider {
        private val customConverters: List<AttributeConverter<*>> = listOf<AttributeConverter<*>>(
        JsonContentConverter(),
    )

    private val customConvertersMap: MutableMap<EnhancedType<*>, AttributeConverter<*>> =
        customConverters.stream().collect(
            Collectors.toMap(
            { obj: AttributeConverter<*> -> obj.type() },
            { c: AttributeConverter<*> -> c }
        ))
    private val defaultProvider: AttributeConverterProvider = DefaultAttributeConverterProvider.create()

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

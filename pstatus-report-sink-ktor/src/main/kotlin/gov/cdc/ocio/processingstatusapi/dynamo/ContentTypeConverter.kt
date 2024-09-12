package gov.cdc.ocio.processingstatusapi.dynamo

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import software.amazon.awssdk.enhanced.dynamodb.*
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.JsonItemAttributeConverter
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.io.UncheckedIOException
import java.util.stream.Collectors


class Content : HashMap<String, Any>()

//class HashMapAttributeConverter : AttributeConverter<Map<String, String>> {
//    /** Default constructor.  */
//    init {
//        mapConverter =
//            MapAttributeConverter.builder(
//                EnhancedType.mapOf(
//                    String::class.java,
//                    String::class.java
//                )
//            )
//                .mapConstructor { mapOf<String, String>() }
//                .keyConverter(StringStringConverter.create())
//                .valueConverter(StringAttributeConverter.create())
//                .build()
//    }
//
//    override fun transformFrom(input: Map<String, String>): AttributeValue {
//        return mapConverter.transformFrom(input)
//    }
//
//    override fun transformTo(input: AttributeValue): Map<String, String> {
//        return mapConverter.transformTo(input)
//    }
//
//    override fun type(): EnhancedType<Map<String, String>> {
//        return mapConverter.type()
//    }
//
//    override fun attributeValueType(): AttributeValueType {
//        return mapConverter.attributeValueType()
//    }
//
//    companion object {
//        private lateinit var mapConverter: AttributeConverter<Map<String, String>>
//    }
//}
class JacksonAttributeConverter<T>(private val clazz: Class<T>) : AttributeConverter<T?> {

    override fun transformFrom(input: T?): AttributeValue {
        try {
            return AttributeValue
                .builder()
                .s(mapper.writeValueAsString(input))
                .build()
        } catch (e: JsonProcessingException) {
            throw UncheckedIOException("Unable to serialize object", e)
        }
    }

    override fun transformTo(input: AttributeValue): T {
        try {
            return mapper.readValue(input.s(), this.clazz)
        } catch (e: JsonProcessingException) {
            throw UncheckedIOException("Unable to parse object", e)
        }
    }

    override fun type(): EnhancedType<T?>? {
        return EnhancedType.of(this.clazz)
    }

    override fun attributeValueType(): AttributeValueType {
        return AttributeValueType.S
    }

    companion object {
        private val mapper = ObjectMapper()

        init {
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        }
    }
}

class JsonContentConverter : AttributeConverter<Content?> {

    private val logger = KotlinLogging.logger {}

    override fun transformFrom(input: Content?): AttributeValue {
        try {
            logger.info("inside transformFrom")
            return AttributeValue.builder().s(objectMapper.writeValueAsString(input)).build()
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Unable to serialize Content to JSON", e)
        }
    }

    override fun transformTo(input: AttributeValue): Content? {
        try {
            logger.info("inside transformTo")
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
    private val customConverters = listOf<AttributeConverter<*>>(
//        JsonContentConverter(),
        JacksonAttributeConverter(Content::class.java),
//        JsonItemAttributeConverter.create(),
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

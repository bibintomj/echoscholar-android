package com.bibintomj.echoscholar.data.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Flexible deserializer that can accept either a single object or a list of objects.
 * Useful when the backend sometimes returns `{}` instead of `[]`.
 */
open class FlexibleListSerializer<T>(
    private val dataSerializer: KSerializer<T>
) : KSerializer<List<T>> {

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("FlexibleList", StructureKind.LIST)

    override fun deserialize(decoder: Decoder): List<T> {
        val input = decoder as? JsonDecoder
            ?: throw IllegalStateException("FlexibleListSerializer can only be used with JsonFormat")

        val element = input.decodeJsonElement()

        return when (element) {
            is JsonArray -> element.map { input.json.decodeFromJsonElement(dataSerializer, it) }
            is JsonObject -> listOf(input.json.decodeFromJsonElement(dataSerializer, element))
            JsonNull -> emptyList()
            else -> throw IllegalStateException("Unexpected JSON element: $element")
        }
    }

    override fun serialize(encoder: Encoder, value: List<T>) {
        encoder.encodeSerializableValue(ListSerializer(dataSerializer), value)
    }
}

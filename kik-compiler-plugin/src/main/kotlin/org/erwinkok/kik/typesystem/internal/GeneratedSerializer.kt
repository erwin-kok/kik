package org.erwinkok.kik.typesystem.internal

import org.erwinkok.kik.typesystem.descriptors.SerialDescriptor
import org.erwinkok.kik.typesystem.encoding.Decoder
import org.erwinkok.kik.typesystem.encoding.Encoder

public interface KSerializer<T> : SerializationStrategy<T>, DeserializationStrategy<T> {
    override val descriptor: SerialDescriptor
}

public interface SerializationStrategy<in T> {
    public val descriptor: SerialDescriptor
    public fun serialize(encoder: Encoder, value: T)
}

public interface DeserializationStrategy<out T> {
    public val descriptor: SerialDescriptor
    public fun deserialize(decoder: Decoder): T
}

@JvmField
internal val EMPTY_SERIALIZER_ARRAY: Array<KSerializer<*>> = arrayOf()

public interface GeneratedSerializer<T> : KSerializer<T> {
    public fun childSerializers(): Array<KSerializer<*>>
    public fun typeParametersSerializers(): Array<KSerializer<*>> = EMPTY_SERIALIZER_ARRAY
}

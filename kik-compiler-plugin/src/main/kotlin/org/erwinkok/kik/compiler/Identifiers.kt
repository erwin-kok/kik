// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler

import org.erwinkok.kik.compiler.KikPackages.internalPackageFqName
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal val KIK_PLUGIN_ORIGIN = IrDeclarationOriginImpl("KIK_TYPESYSTEM", true)

internal object KikAnnotations {
    val kikTypeAnnotationFqName = FqName("org.erwinkok.kik.typesystem.KikType")
    val kikTypePartAnnotationFqName = FqName("org.erwinkok.kik.typesystem.KikTypePart")
    val kikPropertyAnnotationFqName = FqName("org.erwinkok.kik.typesystem.KikProperty")
    val kikInlineAnnotationFqName = FqName("org.erwinkok.kik.typesystem.KikInline")

    val kikTypeAnnotationClassId = ClassId.topLevel(kikTypeAnnotationFqName)
    val kikTypePartAnnotationClassId = ClassId.topLevel(kikTypePartAnnotationFqName)
    val kikPropertyAnnotationClassId = ClassId.topLevel(kikPropertyAnnotationFqName)
    val kikInlineAnnotationClassId = ClassId.topLevel(kikInlineAnnotationFqName)
}

internal object KikPackages {
    val packageFqName = FqName("org.erwinkok.kik.typesystem")
    val internalPackageFqName = FqName("org.erwinkok.kik.typesystem.internal")
    val encodingPackageFqName = FqName("org.erwinkok.kik.typesystem.encoding")
    val descriptorsPackageFqName = FqName("org.erwinkok.kik.typesystem.descriptors")
    val builtinsPackageFqName = FqName("org.erwinkok.kik.typesystem.builtins")

    val allPublicPackages = listOf(packageFqName, encodingPackageFqName, descriptorsPackageFqName, builtinsPackageFqName)
}

internal object KikEntityNames {
    const val KSERIALIZER_CLASS = "KSerializer"
    const val SERIAL_DESC_FIELD = "descriptor"
    const val SAVE = "serialize"
    const val LOAD = "deserialize"
    const val SERIALIZER_CLASS = "\$serializer"

    const val CACHED_SERIALIZER_PROPERTY = "\$cachedSerializer"
    const val CACHED_CHILD_SERIALIZERS_PROPERTY = "\$childSerializers"

    // classes
    val KSERIALIZER_NAME = Name.identifier(KSERIALIZER_CLASS)
    val SERIAL_CTOR_MARKER_NAME = Name.identifier("SerializationConstructorMarker")
    val KSERIALIZER_NAME_FQ = KikPackages.packageFqName.child(KSERIALIZER_NAME)
    val KSERIALIZER_CLASS_ID = ClassId.topLevel(KSERIALIZER_NAME_FQ)

    val SERIALIZER_CLASS_NAME = Name.identifier(SERIALIZER_CLASS)

    val GENERATED_SERIALIZER_CLASS = Name.identifier("GeneratedSerializer")
    val GENERATED_SERIALIZER_FQ = KikPackages.internalPackageFqName.child(GENERATED_SERIALIZER_CLASS)

    val SERIALIZER_FACTORY_INTERFACE_NAME = Name.identifier("SerializerFactory")

    const val ENCODER_CLASS = "Encoder"
    const val STRUCTURE_ENCODER_CLASS = "CompositeEncoder"
    const val DECODER_CLASS = "Decoder"
    const val STRUCTURE_DECODER_CLASS = "CompositeDecoder"

    const val ANNOTATION_MARKER_CLASS = "SerializableWith"

    const val SERIAL_DESCRIPTOR_CLASS = "SerialDescriptor"
    const val SERIAL_DESCRIPTOR_CLASS_IMPL = "PluginGeneratedSerialDescriptor"
    const val SERIAL_DESCRIPTOR_FOR_INLINE = "InlineClassDescriptor"
    const val SERIAL_DESCRIPTOR_FOR_ENUM = "EnumDescriptor"

    //exceptions
    const val MISSING_FIELD_EXC = "MissingFieldException"
    const val UNKNOWN_FIELD_EXC = "UnknownFieldException"

    // functions
    val SERIAL_DESC_FIELD_NAME = Name.identifier(SERIAL_DESC_FIELD)
    val SAVE_NAME = Name.identifier(SAVE)
    val LOAD_NAME = Name.identifier(LOAD)
    val CHILD_SERIALIZERS_GETTER = Name.identifier("childSerializers")
    val TYPE_PARAMS_SERIALIZERS_GETTER = Name.identifier("typeParametersSerializers")
    val WRITE_SELF_NAME = Name.identifier("write\$Self")
    val SERIALIZER_PROVIDER_NAME = Name.identifier("serializer")
    val GENERATED_SERIALIZER_PROVIDER_NAME = Name.identifier("generatedSerializer")
    val SINGLE_MASK_FIELD_MISSING_FUNC_NAME = Name.identifier("throwMissingFieldException")
    val ARRAY_MASK_FIELD_MISSING_FUNC_NAME = Name.identifier("throwArrayMissingFieldException")
    val ENUM_SERIALIZER_FACTORY_FUNC_NAME = Name.identifier("createSimpleEnumSerializer")
    val ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME = Name.identifier("createAnnotatedEnumSerializer")
    val CACHED_SERIALIZER_PROPERTY_NAME = Name.identifier(CACHED_SERIALIZER_PROPERTY)
    val CACHED_CHILD_SERIALIZERS_PROPERTY_NAME = Name.identifier(CACHED_CHILD_SERIALIZERS_PROPERTY)

    // parameters
    val dummyParamName = Name.identifier("serializationConstructorMarker")
    const val typeArgPrefix = "typeSerial"

    val OBJECT_REFERENCE = Name.identifier("kind")
    val SET = Name.special("<set-?>")
    const val SERIALIZE_FUNCTION_NAME = "serialize"
    val KIK_TYPE_INTERFACE_NAME = Name.identifier("KikCommonType")

}

internal object SpecialBuiltins {
    const val referenceArraySerializer = "ReferenceArraySerializer"
    const val enumSerializer = "EnumSerializer"

    object Names {
        val referenceArraySerializer = Name.identifier(SpecialBuiltins.referenceArraySerializer)
    }
}

internal object KikClassIds {
    val kSerializerId = ClassId(KikPackages.packageFqName, KikEntityNames.KSERIALIZER_NAME)
    val enumSerializerId = ClassId(KikPackages.internalPackageFqName, Name.identifier(SpecialBuiltins.enumSerializer))
    val referenceArraySerializerId = ClassId(KikPackages.internalPackageFqName, Name.identifier(SpecialBuiltins.referenceArraySerializer))
    val generatedSerializerId = ClassId(KikPackages.internalPackageFqName, KikEntityNames.GENERATED_SERIALIZER_CLASS)


    val kikCommonTypeClassId = ClassId(internalPackageFqName, KikEntityNames.KIK_TYPE_INTERFACE_NAME)
    val OBJECT_REFERENCE_CLASS = ClassId(internalPackageFqName, Name.identifier("RealmObjectReference"))
}

internal object AnnotationParameterNames {
    val GROUP = Name.identifier("group")
    val VERSION = Name.identifier("version")
    val KIND = Name.identifier("kind")
    val NAME = Name.identifier("name")
    val REQUIRED = Name.identifier("required")
}

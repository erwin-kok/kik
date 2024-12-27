// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.resolve

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
    const val SERIALIZE_FUNCTION_NAME = "serialize"
    const val SERIALIZER_CLASS = "\$serializer"
    const val STRUCTURE_ENCODER_CLASS = "CompositeEncoder"
    const val SERIAL_DESCRIPTOR_CLASS = "SerialDescriptor"

    val KIK_TYPE_INTERFACE_NAME = Name.identifier("KikCommonType")
    val GENERATED_SERIALIZER_CLASS = Name.identifier("GeneratedSerializer")
    val SERIAL_CTOR_MARKER_NAME = Name.identifier("SerializationConstructorMarker")

    val kikCommonTypeClassId = ClassId(KikPackages.internalPackageFqName, KIK_TYPE_INTERFACE_NAME)

    val SERIALIZER_CLASS_NAME = Name.identifier(SERIALIZER_CLASS)
    val WRITE_SELF_NAME = Name.identifier("write\$Self")

    val dummyParamName = Name.identifier("serializationConstructorMarker")
}

internal object KikClassIds {
    val generatedSerializerId = ClassId(KikPackages.internalPackageFqName, KikEntityNames.GENERATED_SERIALIZER_CLASS)
}

internal object AnnotationParameterNames {
    val GROUP = Name.identifier("group")
    val VERSION = Name.identifier("version")
    val KIND = Name.identifier("kind")
    val NAME = Name.identifier("name")
    val REQUIRED = Name.identifier("required")
}

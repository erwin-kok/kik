// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.resolve

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

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

internal object AnnotationParameterNames {
    val GROUP = Name.identifier("group")
    val VERSION = Name.identifier("version")
    val KIND = Name.identifier("kind")
    val NAME = Name.identifier("name")
    val REQUIRED = Name.identifier("required")
}

// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.resolve

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

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

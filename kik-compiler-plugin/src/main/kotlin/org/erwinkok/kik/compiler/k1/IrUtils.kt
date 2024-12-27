// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.erwinkok.kik.compiler.k1

import org.erwinkok.kik.compiler.resolve.KikAnnotations
import org.jetbrains.kotlin.backend.jvm.ir.getStringConstArgument
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.findAnnotation

internal val List<IrConstructorCall>.kikPropertyNameValue: String?
    get() = findAnnotation(KikAnnotations.kikPropertyAnnotationFqName)?.getStringConstArgument(0) // KikProperty(name = "foo")

internal val List<IrConstructorCall>.kikPropertyRequiredValue: Boolean
    get() {
        val annotation = findAnnotation(KikAnnotations.kikPropertyAnnotationFqName) ?: return false
        val value = annotation.getValueArgument(1) as? IrConst ?: return false
        if (value.kind != IrConstKind.Boolean) return false
        return value.value as Boolean
    }

internal val List<IrConstructorCall>.kikInline: IrConstructorCall?
    get() = findAnnotation(KikAnnotations.kikInlineAnnotationFqName)

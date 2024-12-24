// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.erwinkok.kik.compiler.properties

import org.erwinkok.kik.compiler.k1.genericIndex
import org.erwinkok.kik.compiler.k1.kikInline
import org.erwinkok.kik.compiler.k1.kikPropertyNameValue
import org.erwinkok.kik.compiler.k1.kikPropertyRequiredValue
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType

internal class IrKikProperty(
    val ir: IrProperty,
    hasBackingField: Boolean,
    declaresDefaultValue: Boolean,
    val type: IrSimpleType
) : IKikProperty {
    override val name = ir.annotations.kikPropertyNameValue ?: ir.name.asString()
    override val originalDescriptorName = ir.name
    val genericIndex = type.genericIndex
    override val optional = ir.annotations.kikPropertyRequiredValue && declaresDefaultValue
    override val inline = ir.annotations.kikInline != null
    override val transient = ir.annotations.kikPropertyNameValue == null || !hasBackingField
}

internal class IrKikProperties(
    override val kikProperties: List<IrKikProperty>,
    override val isExternallySerializable: Boolean,
    override val kikConstructorProperties: List<IrKikProperty>,
    override val kikStandaloneProperties: List<IrKikProperty>
) : IKikProperties<IrKikProperty> {
    companion object {
        fun fromIrClass(irClass: IrClass): IrKikProperties {
            return IrKikProperties(listOf(), true, listOf(), listOf())
        }
    }
}

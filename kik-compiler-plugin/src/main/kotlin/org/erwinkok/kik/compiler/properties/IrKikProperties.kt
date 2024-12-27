// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.erwinkok.kik.compiler.properties

import org.erwinkok.kik.compiler.k1.kikInline
import org.erwinkok.kik.compiler.k1.kikPropertyNameValue
import org.erwinkok.kik.compiler.k1.kikPropertyRequiredValue
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties

internal class IrKikProperty(
    val property: IrProperty,
    override val isConstructorParameterWithDefault: Boolean,
    declaresDefaultValue: Boolean,
) : IKikProperty {
    override val name = property.annotations.kikPropertyNameValue ?: property.name.asString()
    override val originalDescriptorName = property.name
    override val optional = property.annotations.kikPropertyRequiredValue && declaresDefaultValue
    override val inline = property.annotations.kikInline != null
}

internal class IrKikProperties(
    override val kikProperties: List<IrKikProperty>,
    override val kikConstructorProperties: List<IrKikProperty>,
    override val kikStandaloneProperties: List<IrKikProperty>
) : IKikProperties<IrKikProperty> {
    companion object {
        fun fromIrClass(irClass: IrClass): IrKikProperties {
            val allProperties = irClass.properties.toList()
            val primaryConstructorProperties = buildPrimaryConstructorProperties(irClass, allProperties)
            val (kikConstructorProperties, kikStandaloneProperties) = allProperties
                .filter { isPropertySerializable(it) }
                .map { property ->
                    val isConstructorParameterWithDefault = primaryConstructorProperties.getValue(property)
                    val declaresDefaultValue = hasInitializer(property)
                    IrKikProperty(
                        property,
                        isConstructorParameterWithDefault,
                        declaresDefaultValue || isConstructorParameterWithDefault
                    )
                }
                .partition { it.property in primaryConstructorProperties }
            val kikProperties = kikConstructorProperties + kikStandaloneProperties
            return IrKikProperties(kikProperties, kikConstructorProperties, kikStandaloneProperties)
        }

        private fun hasInitializer(property: IrProperty): Boolean {
            val initializer = property.backingField?.initializer ?: return false
            val expression = initializer.expression as? IrGetValueImpl ?: return false
            return expression.origin != IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
        }

        private fun buildPrimaryConstructorProperties(irClass: IrClass, allProperties: List<IrProperty>): Map<IrProperty, Boolean> {
            val primaryConstructorParams = irClass.primaryConstructor?.valueParameters.orEmpty()
            val parameterMap = primaryConstructorParams.associateBy { it.name }
            val result = mutableMapOf<IrProperty, Boolean>()
            for (property in allProperties) {
                val parameter = parameterMap[property.name]
                if (parameter != null) {
                    result[property] = parameter.hasDefaultValue()
                }
            }
            return result.withDefault { false }
        }

        private fun isPropertySerializable(property: IrProperty): Boolean {
            if (property.isFakeOverride || property.isDelegated || property.origin == IrDeclarationOrigin.DELEGATED_MEMBER) {
                return false
            }
            if (property.annotations.kikPropertyNameValue == null) {
                return false
            }
            val hasBackingField = when (property.origin) {
                IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> false
                else -> property.backingField != null
            }
            return hasBackingField
        }
    }
}

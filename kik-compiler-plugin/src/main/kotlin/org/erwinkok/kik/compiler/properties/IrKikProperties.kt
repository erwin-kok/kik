// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.erwinkok.kik.compiler.properties

import org.erwinkok.kik.compiler.k1.kikInline
import org.erwinkok.kik.compiler.k1.kikPropertyNameValue
import org.erwinkok.kik.compiler.k1.kikPropertyRequiredValue
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.deserialization.registeredInSerializationPluginMetadataExtension
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.psi.KtDeclarationWithInitializer
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor

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
                    val isPropertyFromAnotherModuleDeclaresDefaultValue = analyzeIfFromAnotherModule1(property)
                    IrKikProperty(
                        property,
                        isConstructorParameterWithDefault,
                        declaresDefaultValue || isConstructorParameterWithDefault || isPropertyFromAnotherModuleDeclaresDefaultValue
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
            val isPropertyWithBackingFieldFromAnotherModule = analyzeIfFromAnotherModule2(property)
            val hasBackingField = when (property.origin) {
                IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> isPropertyWithBackingFieldFromAnotherModule
                else -> property.backingField != null
            }
            return hasBackingField
        }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        private fun analyzeIfFromAnotherModule1(property: IrProperty): Boolean {
            if (property.descriptor is DeserializedPropertyDescriptor) {
                // IrLazyProperty does not deserialize backing fields correctly, so we should fall back to info from descriptor.
                // DeserializedPropertyDescriptor can be encountered only after K1, so it is safe to check it.
                return property.descriptor.declaresDefaultValue()
            }
            if (property !is Fir2IrLazyProperty) {
                return false
            }
            // Deserialized properties don't contain information about backing field, so we should extract this information from the
            // attribute, which is set if the property was mentioned in SerializationPluginMetadataExtensions.
            // Also, deserialized properties do not store default value (initializer expression) for property,
            // so we either should find corresponding constructor parameter and check its default, or rely on less strict check for default getter.
            // Comments are copied from PropertyDescriptor.declaresDefaultValue() as it has similar logic.
            val matchingPrimaryConstructorParam = property.containingClass?.declarations?.filterIsInstance<FirPrimaryConstructor>()
                ?.singleOrNull()?.valueParameters?.find { it.name == property.name }
            return if (matchingPrimaryConstructorParam != null) {
                // If property is a constructor parameter, check parameter default value
                // (serializable classes always have parameters-as-properties, so no name clash here)
                matchingPrimaryConstructorParam.defaultValue != null
            } else {
                // If it is a body property, then it is likely to have initializer when getter is not specified
                // note this approach is not working well if we have smth like `get() = field`, but such cases on cross-module boundaries
                // should be very marginal. If we want to solve them, we need to add protobuf metadata extension.
                property.fir.getter is FirDefaultPropertyGetter
            }
        }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        private fun analyzeIfFromAnotherModule2(property: IrProperty): Boolean {
            if (property.descriptor is DeserializedPropertyDescriptor) {
                // IrLazyProperty does not deserialize backing fields correctly, so we should fall back to info from descriptor.
                // DeserializedPropertyDescriptor can be encountered only after K1, so it is safe to check it.
                return property.descriptor.backingField != null || property.descriptor.declaresDefaultValue()
            }
            if (property !is Fir2IrLazyProperty) {
                return false
            }
            // Deserialized properties don't contain information about backing field, so we should extract this information from the
            // attribute, which is set if the property was mentioned in SerializationPluginMetadataExtensions.
            // Also, deserialized properties do not store default value (initializer expression) for property,
            // so we either should find corresponding constructor parameter and check its default, or rely on less strict check for default getter.
            // Comments are copied from PropertyDescriptor.declaresDefaultValue() as it has similar logic.
            return property.fir.symbol.registeredInSerializationPluginMetadataExtension
        }

        private fun PropertyDescriptor.declaresDefaultValue(): Boolean {
            when (val declaration = this.source.getPsi()) {
                is KtDeclarationWithInitializer -> return declaration.initializer != null
                is KtParameter -> return declaration.defaultValue != null
                is Any -> return false // Not-null check
            }
            // PSI is null, property is from another module
            if (this !is DeserializedPropertyDescriptor) return false
            val myClassCtor = (this.containingDeclaration as? ClassDescriptor)?.unsubstitutedPrimaryConstructor ?: return false
            // If property is a constructor parameter, check parameter default value
            // (serializable classes always have parameters-as-properties, so no name clash here)
            if (myClassCtor.valueParameters.find { it.name == this.name }?.declaresDefaultValue() == true) return true
            // If it is a body property, then it is likely to have initializer when getter is not specified
            // note this approach is not working well if we have smth like `get() = field`, but such cases on cross-module boundaries
            // should be very marginal. If we want to solve them, we need to add protobuf metadata extension.
            if (getter?.isDefault == true) return true
            return false
        }
    }
}

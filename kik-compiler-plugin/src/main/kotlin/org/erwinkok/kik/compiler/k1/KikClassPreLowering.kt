// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.erwinkok.kik.compiler.k1

import org.erwinkok.kik.compiler.KIK_PLUGIN_ORIGIN
import org.erwinkok.kik.compiler.KikClassIds.kikCommonTypeClassId
import org.erwinkok.kik.compiler.KikEntityNames
import org.erwinkok.kik.compiler.properties.IrKikProperties
import org.erwinkok.kik.compiler.properties.bitMaskSlotCount
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.isSingleFieldValueClass
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

internal class KikClassPreLowering(
    pluginContext: IrPluginContext
) : IrElementTransformerVoid(), ClassLoweringPass {
    private val compilerContext = KikPluginContext(pluginContext)

    override fun lower(irClass: IrClass) {
        if (!irClass.isSerializable) {
            return
        }
        if (irClass.isSingleFieldValueClass) {
            return
        }
        val commonTypeSymbol = compilerContext.lookupClassOrThrow(kikCommonTypeClassId).symbol
        irClass.superTypes += commonTypeSymbol.defaultType

        preGenerateWriteSelfMethodIfNeeded(irClass)
        preGenerateDeserializationConstructorIfNeeded(irClass)
    }

    private fun preGenerateWriteSelfMethodIfNeeded(irClass: IrClass) {
        val serializerClass = irClass.classSerializer()?.owner ?: return

        if (serializerClass.findPluginGeneratedMethod(KikEntityNames.SERIALIZE_FUNCTION_NAME) == null) {
            return
        }
        if (irClass.findWriteSelfMethod() != null) {
            return
        }
        val method = irClass.addFunction {
            name = KikEntityNames.WRITE_SELF_NAME
            returnType = compilerContext.irBuiltIns.unitType
            visibility = if (irClass.modality == Modality.FINAL) DescriptorVisibilities.INTERNAL else DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            origin = KIK_PLUGIN_ORIGIN
        }

        // object
        method.addValueParameter(
            Name.identifier("self"),
            irClass.typeWith(),
            KIK_PLUGIN_ORIGIN
        )
        // encoder
        method.addValueParameter(
            Name.identifier("output"),
            compilerContext.getClassFromRuntime(KikEntityNames.STRUCTURE_ENCODER_CLASS).defaultType,
            KIK_PLUGIN_ORIGIN
        )
        // descriptor
        val serialDescriptorSymbol = compilerContext.getClassFromRuntime(KikEntityNames.SERIAL_DESCRIPTOR_CLASS)
        method.addValueParameter(
            Name.identifier("serialDesc"),
            serialDescriptorSymbol.defaultType,
            KIK_PLUGIN_ORIGIN
        )

        val referenceClass = compilerContext.referenceClass(StandardClassIds.Annotations.jvmStatic)
        if (referenceClass != null) {
            val annotationCtor = referenceClass.constructors.single<IrConstructorSymbol> { it.owner.isPrimary }
            val annotationType = referenceClass.defaultType
            method.annotations += IrConstructorCallImpl.fromSymbolOwner(method.startOffset, method.endOffset, annotationType, annotationCtor)
        }

        compilerContext.metadataDeclarationRegistrar.registerFunctionAsMetadataVisible(method)
    }

    private fun preGenerateDeserializationConstructorIfNeeded(irClass: IrClass) {
        if (irClass.findSerializableSyntheticConstructor() != null) {
            return
        }
        val ctor = irClass.addConstructor {
            origin = KIK_PLUGIN_ORIGIN
            visibility = if (irClass.modality == Modality.FINAL) DescriptorVisibilities.INTERNAL else DescriptorVisibilities.PUBLIC
        }
        val markerClassSymbol = compilerContext.getClassFromInternalSerializationPackage(KikEntityNames.SERIAL_CTOR_MARKER_NAME.asString())
        val serializableProperties = IrKikProperties.fromIrClass(irClass).kikProperties
        val bitMaskSlotsCount = serializableProperties.bitMaskSlotCount()

        repeat(bitMaskSlotsCount) {
            ctor.addValueParameter(Name.identifier("seen$it"), compilerContext.irBuiltIns.intType, KIK_PLUGIN_ORIGIN)
        }

        for (kikProperty in serializableProperties) {
            // SerialName can contain illegal identifier characters, so we use original source code name for parameter
            val type = kikProperty.property.getter!!.returnType as IrSimpleType
            ctor.addValueParameter(kikProperty.originalDescriptorName, type.makeNullableIfNotPrimitive(), KIK_PLUGIN_ORIGIN)
        }

        ctor.addValueParameter(KikEntityNames.dummyParamName, markerClassSymbol.defaultType.makeNullable(), KIK_PLUGIN_ORIGIN)

        compilerContext.metadataDeclarationRegistrar.registerConstructorAsMetadataVisible(ctor)
    }

    private fun IrType.makeNullableIfNotPrimitive(): IrType {
        return if (this.isPrimitiveType(false)) this
        else this.makeNullable()
    }
}

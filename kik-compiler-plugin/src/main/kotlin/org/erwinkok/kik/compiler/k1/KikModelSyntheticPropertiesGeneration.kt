// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.erwinkok.kik.compiler.k1

import org.erwinkok.kik.compiler.KikClassIds.OBJECT_REFERENCE_CLASS
import org.erwinkok.kik.compiler.KikClassIds.kikCommonTypeClassId
import org.erwinkok.kik.compiler.KikEntityNames
import org.erwinkok.kik.compiler.KikEntityNames.OBJECT_REFERENCE
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.IrFieldBuilder
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.IrPropertyBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addSetter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.name.Name

internal class KikModelSyntheticPropertiesGeneration(private val pluginContext: IrPluginContext) {
    private val objectReferenceClass = pluginContext.lookupClassOrThrow(OBJECT_REFERENCE_CLASS)
    private val realmModelInternalInterface = pluginContext.lookupClassOrThrow(kikCommonTypeClassId)

    fun addKikCommonTypeInternalProperties(irClass: IrClass): IrClass {
        val type = objectReferenceClass.typeWith(irClass.defaultType).makeNullable()
        return irClass.apply {
            addInternalVarProperty(
                realmModelInternalInterface,
                OBJECT_REFERENCE,
                type,
                ::irNull
            )
        }
    }

    @Suppress("LongMethod")
    private fun IrClass.addInternalVarProperty(
        owner: IrClass,
        propertyName: Name,
        propertyType: IrType,
        initExpression: (startOffset: Int, endOffset: Int) -> IrExpressionBody
    ) {
        // PROPERTY name:realmPointer visibility:public modality:OPEN [var]
        // Also add @kotlin.
        val property = addProperty {
            at(this@addInternalVarProperty.startOffset, this@addInternalVarProperty.endOffset)
            name = propertyName
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.OPEN
            isVar = true
        }
        // FIELD PROPERTY_BACKING_FIELD name:objectPointer type:kotlin.Long? visibility:private
        property.backingField = pluginContext.irFactory.buildField {
            at(this@addInternalVarProperty.startOffset, this@addInternalVarProperty.endOffset)
            name = property.name
            visibility = DescriptorVisibilities.PRIVATE
            modality = property.modality
            type = propertyType
        }.apply {
            // EXPRESSION_BODY
            //  CONST Boolean type=kotlin.Boolean value=false
            initializer = initExpression(startOffset, endOffset)
        }
        property.backingField?.parent = this
        property.backingField?.correspondingPropertySymbol = property.symbol

        // FUN DEFAULT _PROPERTY_ACCESSOR name:<get-objectPointer> visibility:public modality:OPEN <> ($this:dev.nhachicha.Foo.$RealmHandler) returnType:kotlin.Long?
        // correspondingProperty: PROPERTY name:objectPointer visibility:public modality:OPEN [var]
        val getter = property.addGetter {
            at(this@addInternalVarProperty.startOffset, this@addInternalVarProperty.endOffset)
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.OPEN
            returnType = propertyType
        }
        // $this: VALUE_PARAMETER name:<this> type:dev.nhachicha.Foo.$RealmHandler
        getter.dispatchReceiverParameter = thisReceiver!!.copyTo(getter)
        // overridden:
        //   public abstract fun <get-realmPointer> (): kotlin.Long? declared in dev.nhachicha.RealmObjectInternal
        val propertyAccessorGetter = owner.getPropertyGetter(propertyName.asString()) ?: error("${propertyName.asString()} function getter symbol is not available")
        getter.overriddenSymbols = listOf(propertyAccessorGetter)

        // BLOCK_BODY
        // RETURN type=kotlin.Nothing from='public final fun <get-objectPointer> (): kotlin.Long? declared in dev.nhachicha.Foo.$RealmHandler'
        // GET_FIELD 'FIELD PROPERTY_BACKING_FIELD name:objectPointer type:kotlin.Long? visibility:private' type=kotlin.Long? origin=null
        // receiver: GET_VAR '<this>: dev.nhachicha.Foo.$RealmHandler declared in dev.nhachicha.Foo.$RealmHandler.<get-objectPointer>' type=dev.nhachicha.Foo.$RealmHandler origin=null
        getter.body = pluginContext.blockBody(getter.symbol) {
            at(startOffset, endOffset)
            +irReturn(
                irGetField(
                    irGet(getter.dispatchReceiverParameter!!),
                    property.backingField!!,
                    property.backingField!!.type
                )
            )
        }

        // FUN DEFAULT_PROPERTY_ACCESSOR name:<set-realmPointer> visibility:public modality:OPEN <> ($this:dev.nhachicha.Child, <set-?>:kotlin.Long?) returnType:kotlin.Unit
        //  correspondingProperty: PROPERTY name:realmPointer visibility:public modality:OPEN [var]
        val setter = property.addSetter {
            at(this@addInternalVarProperty.startOffset, this@addInternalVarProperty.endOffset)
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.OPEN
            returnType = pluginContext.irBuiltIns.unitType
        }
        // $this: VALUE_PARAMETER name:<this> type:dev.nhachicha.Child
        setter.dispatchReceiverParameter = thisReceiver!!.copyTo(setter)
        setter.correspondingPropertySymbol = property.symbol

        // overridden:
        //  public abstract fun <set-realmPointer> (<set-?>: kotlin.Long?): kotlin.Unit declared in dev.nhachicha.RealmObjectInternal
        val realmPointerSetter = owner.getPropertySetter(propertyName.asString()) ?: error("${propertyName.asString()} function getter symbol is not available")
        setter.overriddenSymbols = listOf(realmPointerSetter)

        // VALUE_PARAMETER name:<set-?> index:0 type:kotlin.Long?
        // BLOCK_BODY
        //  SET_FIELD 'FIELD PROPERTY_BACKING_FIELD name:realmPointer type:kotlin.Long? visibility:private' type=kotlin.Unit origin=null
        //  receiver: GET_VAR '<this>: io.realm.example.Sample declared in io.realm.example.Sample.<set-realmPointer>' type=io.realm.example.Sample origin=null
        //  value: GET_VAR '<set-?>: kotlin.Long? declared in io.realm.example.Sample.<set-realmPointer>' type=kotlin.Long? origin=null
        val valueParameter = setter.addValueParameter {
            this.name = KikEntityNames.SET
            this.type = propertyType
        }
        setter.body = DeclarationIrBuilder(pluginContext, setter.symbol).irBlockBody {
            at(startOffset, endOffset)
            +irSetField(
                irGet(setter.dispatchReceiverParameter!!),
                property.backingField!!.symbol.owner,
                irGet(valueParameter),
            )
        }
    }

    private fun irNull(startOffset: Int, endOffset: Int): IrExpressionBody =
        pluginContext.irFactory.createExpressionBody(
            startOffset,
            endOffset,
            IrConstImpl.constNull(startOffset, endOffset, pluginContext.irBuiltIns.nothingNType)
        )

    internal fun IrFunctionBuilder.at(startOffset: Int, endOffset: Int) = also {
        this.startOffset = startOffset
        this.endOffset = endOffset
    }

    internal fun IrFieldBuilder.at(startOffset: Int, endOffset: Int) = also {
        this.startOffset = startOffset
        this.endOffset = endOffset
    }

    internal fun IrPropertyBuilder.at(startOffset: Int, endOffset: Int) = also {
        this.startOffset = startOffset
        this.endOffset = endOffset
    }

}

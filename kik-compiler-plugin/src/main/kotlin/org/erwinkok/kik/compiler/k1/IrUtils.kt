// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.erwinkok.kik.compiler.k1

import org.erwinkok.kik.compiler.KikAnnotations
import org.erwinkok.kik.compiler.KikEntityNames
import org.erwinkok.kik.compiler.KikPackages
import org.erwinkok.kik.compiler.k2.KikPluginKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.getStringConstArgument
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.name.ClassId

internal val IrClass.isSerializable: Boolean
    get() {
        if (kind != ClassKind.CLASS) return false
        if (getAnnotation(KikAnnotations.kikTypePartAnnotationFqName) != null) return true
        return getAnnotation(KikAnnotations.kikTypeAnnotationFqName) != null
    }

private fun IrClass.checkSerializableOrMetaAnnotationArgs(): Boolean {
    val annot = getAnnotation(KikAnnotations.kikTypeAnnotationFqName)
    if (annot != null) {
        return annot.getValueArgument(0) == null
    }
    return false
}

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

internal fun IrClass?.classSerializer(): IrClassSymbol? = this?.let {
    // default serializable?
    if (shouldHaveGeneratedSerializer()) {
        // $serializer nested class
        return this.generatedSerializer
    }
    return null
}

internal val IrClass.generatedSerializer: IrClassSymbol?
    get() = declarations
        .filterIsInstance<IrClass>()
        .singleOrNull { it.name == KikEntityNames.SERIALIZER_CLASS_NAME }?.symbol

internal fun IrClass.shouldHaveGeneratedSerializer(): Boolean =
    (isSerializable && (modality == Modality.FINAL || modality == Modality.OPEN)) || (isSerializable && kind != ClassKind.ENUM_CLASS)

internal fun IrClass.findPluginGeneratedMethod(name: String): IrSimpleFunction? {
    return this.functions.find {
        it.name.asString() == name && it.isFromPlugin()
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun IrDeclaration.isFromPlugin(): Boolean =
    this.origin == IrDeclarationOrigin.GeneratedByPlugin(KikPluginKey)

internal fun IrClass.findWriteSelfMethod(): IrSimpleFunction? =
    functions.singleOrNull { it.name == KikEntityNames.WRITE_SELF_NAME && !it.isFakeOverride }

internal fun IrClass.findSerializableSyntheticConstructor(): IrConstructorSymbol? {
    return declarations.filterIsInstance<IrConstructor>().singleOrNull { it.isSerializationCtor() }?.symbol
}

internal fun IrConstructor.isSerializationCtor(): Boolean {
    return valueParameters.lastOrNull()?.run {
        name == KikEntityNames.dummyParamName && type.classFqName == KikPackages.internalPackageFqName.child(KikEntityNames.SERIAL_CTOR_MARKER_NAME)
    } == true
}

internal fun IrPluginContext.lookupClassOrThrow(name: ClassId): IrClass {
    return referenceClass(name)?.owner
        ?: error("Cannot find ${name.asString()} on platform $platform.")
}

internal fun IrPluginContext.blockBody(
    symbol: IrSymbol,
    block: IrBlockBodyBuilder.() -> Unit
): IrBlockBody =
    DeclarationIrBuilder(this, symbol).irBlockBody { block() }

// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.erwinkok.kik.compiler.k1

import org.erwinkok.kik.compiler.k2.FirKikPluginKey
import org.erwinkok.kik.compiler.resolve.KikAnnotations
import org.erwinkok.kik.compiler.resolve.KikEntityNames
import org.erwinkok.kik.compiler.resolve.KikPackages
import org.jetbrains.kotlin.backend.jvm.ir.getBooleanConstArgument
import org.jetbrains.kotlin.backend.jvm.ir.getStringConstArgument
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation

internal val IrClass.isInternalSerializable: Boolean
    get() {
        if (kind != ClassKind.CLASS) return false
        return hasAnnotation(KikAnnotations.kikTypePartAnnotationFqName)
    }

internal fun IrClass.shouldHaveGeneratedSerializer(): Boolean =
    (isInternalSerializable && (modality == Modality.FINAL || modality == Modality.OPEN))
            // enum factory must be used for enums
            || (isInternalSerializable && kind != ClassKind.ENUM_CLASS)

internal val IrClass.generatedSerializer: IrClassSymbol?
    get() = declarations
        .filterIsInstance<IrClass>()
        .singleOrNull { it.name == KikEntityNames.KIK_CLASS_NAME }?.symbol

internal fun IrClass.findPluginGeneratedMethod(name: String): IrSimpleFunction? {
    return this.functions.find {
        it.name.asString() == name && it.isFromPlugin()
    }
}

internal val IrType.genericIndex: Int?
    get() = (this.classifierOrNull as? IrTypeParameterSymbol)?.owner?.index

internal val List<IrConstructorCall>.kikPropertyNameValue: String?
    get() = findAnnotation(KikAnnotations.kikPropertyAnnotationFqName)?.getStringConstArgument(0) // KikProperty(name = "foo")

internal val List<IrConstructorCall>.kikPropertyRequiredValue: Boolean
    get() = findAnnotation(KikAnnotations.kikPropertyAnnotationFqName)?.getBooleanConstArgument(1) == true // KikProperty(required = true)

internal val List<IrConstructorCall>.kikInline: IrConstructorCall?
    get() = findAnnotation(KikAnnotations.kikInlineAnnotationFqName)

internal fun IrDeclaration.isFromPlugin(): Boolean =
    this.origin == IrDeclarationOrigin.GeneratedByPlugin(FirKikPluginKey)

internal fun IrClass.findWriteSelfMethod(): IrSimpleFunction? =
    functions.singleOrNull { it.name == KikEntityNames.WRITE_SELF_NAME && !it.isFakeOverride }

internal fun IrClass.findSerializableSyntheticConstructor(): IrConstructorSymbol? {
    return declarations.filterIsInstance<IrConstructor>().singleOrNull { it.isSerializationCtor() }?.symbol
}

internal fun IrConstructor.isSerializationCtor(): Boolean {
    /*kind == CallableMemberDescriptor.Kind.SYNTHESIZED does not work because DeserializedClassConstructorDescriptor loses its kind*/
    return valueParameters.lastOrNull()?.run {
        name == KikEntityNames.dummyParamName && type.classFqName == KikPackages.internalPackageFqName.child(
            KikEntityNames.SERIAL_CTOR_MARKER_NAME
        )
    } == true
}

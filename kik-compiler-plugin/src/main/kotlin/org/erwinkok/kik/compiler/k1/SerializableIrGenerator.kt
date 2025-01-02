@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.erwinkok.kik.compiler.k1

import org.erwinkok.kik.compiler.KIK_PLUGIN_ORIGIN
import org.erwinkok.kik.compiler.KikEntityNames
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.isSingleFieldValueClass
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.patchDeclarationParents

internal class SerializableIrGenerator(
    val irClass: IrClass,
    context: KikPluginContext
) : IrBuilder(context) {
    fun generate() {
        generateSyntheticInternalConstructor()
        generateSyntheticMethods()
    }

    private fun generateSyntheticInternalConstructor() {
        val serializerDescriptor = irClass.classSerializer()?.owner ?: return

        if (!irClass.isSingleFieldValueClass && serializerDescriptor.findPluginGeneratedMethod(KikEntityNames.LOAD) != null) {
            val constrDesc = irClass.constructors.find(IrConstructor::isSerializationCtor) ?: return
            generateInternalConstructor(constrDesc)
        }
    }

    private fun generateSyntheticMethods() {
        val serializerDescriptor = irClass.classSerializer()?.owner ?: return

        if (!irClass.isSingleFieldValueClass && serializerDescriptor.findPluginGeneratedMethod(KikEntityNames.SAVE) != null) {
            val func = irClass.findWriteSelfMethod() ?: return
            func.origin = KIK_PLUGIN_ORIGIN
            generateWriteSelfMethod(func)
        }
    }

    private fun generateInternalConstructor(constructorDescriptor: IrConstructor) {
        addFunctionBody(constructorDescriptor) { ctor ->
        }
    }

    private fun generateWriteSelfMethod(methodDescriptor: IrSimpleFunction) {
        addFunctionBody(methodDescriptor) { writeSelfFunction ->
        }
    }

    companion object {
        fun generate(
            irClass: IrClass,
            context: KikPluginContext,
        ) {
            if (irClass.isSerializable) {
                SerializableIrGenerator(irClass, context).generate()
                irClass.patchDeclarationParents(irClass.parent)
            }
        }
    }
}
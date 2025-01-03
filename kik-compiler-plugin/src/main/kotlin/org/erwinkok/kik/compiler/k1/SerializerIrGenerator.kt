@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.erwinkok.kik.compiler.k1

import org.erwinkok.kik.compiler.KIK_PLUGIN_ORIGIN
import org.erwinkok.kik.compiler.KikEntityNames
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.patchDeclarationParents

internal class SerializerIrGenerator(
    val irClass: IrClass,
    compilerContext: KikPluginContext
) : IrBuilder(compilerContext) {
    fun generate() {
        irClass.findPluginGeneratedProperty(KikEntityNames.SERIAL_DESC_FIELD)?.let { generateSerialDescriptor(it) }

        irClass.findPluginGeneratedMethod(KikEntityNames.SAVE)?.let { generateSave(it) }
        irClass.findPluginGeneratedMethod(KikEntityNames.LOAD)?.let { generateLoad(it) }
        irClass.findPluginGeneratedMethod(KikEntityNames.CHILD_SERIALIZERS_GETTER.identifier)?.let { generateChildSerializersGetter(it) }
        irClass.findPluginGeneratedMethod(KikEntityNames.TYPE_PARAMS_SERIALIZERS_GETTER.identifier)?.let { generateTypeParamsSerializersGetter(it) }
    }

    private fun generateSerialDescriptor(generatedSerialDescPropertyDescriptor: IrProperty) {

    }

    private fun generateSave(function: IrSimpleFunction) {
        addFunctionBody(function) { saveFunc ->
        }
    }

    private fun generateLoad(function: IrSimpleFunction) {
        addFunctionBody(function) { loadFunc ->
        }
    }

    private fun generateChildSerializersGetter(function: IrSimpleFunction) {
        addFunctionBody(function) { irFun ->
        }
    }

    private fun generateTypeParamsSerializersGetter(function: IrSimpleFunction) {
        addFunctionBody(function) { irFun ->
        }
    }

    companion object {
        fun generate(
            irClass: IrClass,
            context: KikPluginContext,
        ) {
            SerializerIrGenerator(irClass, context).generate()
            if (irClass.isFromPlugin()) {
                // replace origin only for plugin generated serializers
                irClass.origin = KIK_PLUGIN_ORIGIN
            }
            irClass.addDefaultConstructorBodyIfAbsent(context)
            irClass.patchDeclarationParents(irClass.parent)
        }
    }
}

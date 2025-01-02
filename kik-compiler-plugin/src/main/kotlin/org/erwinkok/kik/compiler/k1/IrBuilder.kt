package org.erwinkok.kik.compiler.k1

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrFunction

internal open class IrBuilder(val compilerContext: KikPluginContext) {
    fun <F : IrFunction> addFunctionBody(function: F, bodyGen: IrBlockBodyBuilder.(F) -> Unit) {
        val parentClass = function.parent
        val startOffset = function.startOffset.takeIf { it >= 0 } ?: parentClass.startOffset
        val endOffset = function.endOffset.takeIf { it >= 0 } ?: parentClass.endOffset
        function.body = DeclarationIrBuilder(compilerContext, function.symbol, startOffset, endOffset).irBlockBody(
            startOffset,
            endOffset
        ) { bodyGen(function) }
    }
}

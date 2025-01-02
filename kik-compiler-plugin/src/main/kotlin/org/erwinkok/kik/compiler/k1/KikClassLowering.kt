// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k1

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal class KikClassLowering(
    pluginContext: IrPluginContext
) : IrElementTransformerVoid(), ClassLoweringPass {
    private val compilerContext = KikPluginContext(pluginContext)

    override fun lower(irClass: IrClass) {
        SerializableIrGenerator.generate(irClass, compilerContext)
        SerializerIrGenerator.generate(irClass, compilerContext)
        SerializableCompanionIrGenerator.generate(irClass, compilerContext)
    }
}

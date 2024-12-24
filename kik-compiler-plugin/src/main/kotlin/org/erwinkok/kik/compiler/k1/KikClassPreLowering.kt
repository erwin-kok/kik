// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.erwinkok.kik.compiler.k1

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal class KikClassPreLowering(
    baseContext: IrPluginContext
) : IrElementTransformerVoid(), ClassLoweringPass {
    override fun lower(irClass: IrClass) {
    }
}

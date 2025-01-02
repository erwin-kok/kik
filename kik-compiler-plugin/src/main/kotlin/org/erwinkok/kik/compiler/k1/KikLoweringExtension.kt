// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k1

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class KikLoweringExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val pass1 = KikClassPreLowering(pluginContext)
        moduleFragment.files.forEach { file ->
            pass1.runOnFilePostfix(file)
        }

        val pass2 = KikClassLowering(pluginContext)
        moduleFragment.files.forEach { file ->
            pass2.runOnFilePostfix(file)
        }
    }
}

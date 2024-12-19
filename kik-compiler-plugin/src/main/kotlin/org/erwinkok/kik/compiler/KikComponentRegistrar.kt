@file:OptIn(ExperimentalCompilerApi::class)

package org.erwinkok.kik.compiler

import com.google.auto.service.AutoService
import org.erwinkok.kik.compiler.KikCommandLineProcessor.Companion.KEY_ENABLED
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(CompilerPluginRegistrar::class)
class SerializationComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (configuration[KEY_ENABLED] == false) {
            return
        }
    }

    override val supportsK2: Boolean
        get() = true
}

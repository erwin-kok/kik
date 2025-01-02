@file:OptIn(ExperimentalCompilerApi::class)

package org.erwinkok.kik.compiler

import com.google.auto.service.AutoService
import org.erwinkok.kik.compiler.KikCommandLineProcessor.Companion.KEY_ENABLED
import org.erwinkok.kik.compiler.k1.KikLoweringExtension
import org.erwinkok.kik.compiler.k2.FirKikExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@AutoService(CompilerPluginRegistrar::class)
class SerializationComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val messageCollector = configuration.get(CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY)

        if (configuration[KEY_ENABLED] == false) {
            messageCollector!!.report(CompilerMessageSeverity.INFO, "The Kik compiler-plugin is not enabled.")
            return
        }

        if (!configuration.languageVersionSettings.languageVersion.usesK2) {
            messageCollector!!.report(CompilerMessageSeverity.ERROR, "The Kik compiler-plugin currently only supports the Kotlin K2 compiler. Please upgrade to Kotlin 2.x.")
            return
        }

        FirExtensionRegistrarAdapter.registerExtension(FirKikExtensionRegistrar())
        IrGenerationExtension.registerExtension(KikLoweringExtension())
    }

    override val supportsK2: Boolean
        get() = true
}

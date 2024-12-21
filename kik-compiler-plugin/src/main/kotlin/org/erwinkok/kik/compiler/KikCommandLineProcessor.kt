@file:OptIn(ExperimentalCompilerApi::class)

package org.erwinkok.kik.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import kotlin.text.toBoolean

@AutoService(CommandLineProcessor::class)
class KikCommandLineProcessor : CommandLineProcessor {
    override val pluginId = "org.erwinkok.kik.kik-compiler-plugin"
    override val pluginOptions = listOf(ENABLED_OPTION, DEBUG_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        ENABLED_OPTION -> configuration.put(KEY_ENABLED, value.toBoolean())
        DEBUG_OPTION -> configuration.put(KEY_DEBUG, value.toBoolean())
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }

    internal companion object {
        internal val KEY_ENABLED = CompilerConfigurationKey<Boolean>("Enable/disable the kik compiler plugin on the given compilation")
        internal val KEY_DEBUG = CompilerConfigurationKey<Boolean>("Enable/disable debug logging on the given compilation")

        val ENABLED_OPTION =
            CliOption(
                optionName = "enabled",
                valueDescription = "<true | false>",
                description = KEY_ENABLED.toString(),
                required = true,
                allowMultipleOccurrences = false,
            )
        val DEBUG_OPTION =
            CliOption(
                optionName = "debug",
                valueDescription = "<true | false>",
                description = KEY_DEBUG.toString(),
                required = false,
                allowMultipleOccurrences = false,
            )
    }
}

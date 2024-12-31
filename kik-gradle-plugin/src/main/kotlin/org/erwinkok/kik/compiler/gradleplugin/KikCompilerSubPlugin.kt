// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.gradleplugin

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

internal class KikCompilerSubPlugin : KotlinCompilerPluginSupportPlugin {
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(KikCompilerGradleConfiguration::class.java)
        return project.provider {
            listOf(
                SubpluginOption("enabled", extension.enabled.toString()),
            )
        }
    }

    override fun getCompilerPluginId(): String {
        return "org.erwinkok.kik.kik-compiler-plugin"
    }

    override fun getPluginArtifact(): SubpluginArtifact {
        return SubpluginArtifact(KIK_COMPILER_GROUP_NAME, KIK_COMPILER_ARTIFACT_NAME)
    }

    override fun getPluginArtifactForNative(): SubpluginArtifact? {
        return SubpluginArtifact(KIK_COMPILER_GROUP_NAME, KIK_COMPILER_ARTIFACT_NATIVE_NAME)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return true
    }

    companion object {
        const val KIK_COMPILER_GROUP_NAME = "org.erwinkok.kik"
        const val KIK_COMPILER_ARTIFACT_NAME = "kik-compiler-plugin"
        const val KIK_COMPILER_ARTIFACT_NATIVE_NAME = "kik-compiler-plugin-native"
    }
}
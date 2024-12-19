// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.jvm.java

class KikCompilerGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            extensions.create("kikCompiler", KikCompilerGradleConfiguration::class.java)
            pluginManager.apply(KikCompilerSubPlugin::class.java)
        }
    }
}

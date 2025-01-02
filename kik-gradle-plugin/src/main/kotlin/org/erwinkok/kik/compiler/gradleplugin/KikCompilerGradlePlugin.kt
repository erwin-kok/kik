// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class KikCompilerGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            extensions.create("kik", KikCompilerGradleConfiguration::class.java)
            pluginManager.apply(KikCompilerSubPlugin::class.java)
        }
    }
}

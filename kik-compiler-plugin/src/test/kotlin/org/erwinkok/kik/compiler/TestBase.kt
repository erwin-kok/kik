// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:OptIn(ExperimentalCompilerApi::class)

package org.erwinkok.kik.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.apply
import kotlin.collections.asList
import kotlin.collections.plus

//
// These test Annotations (needed to successfully test the compiler plugin) must be synced with the
// annotations defined in org.erwinkok.kik.typesystem.Annotations.kt
//
internal val annotations =
    kotlin(
        "Annotations.kt",
        """
            package org.erwinkok.kik.typesystem
            
            @MustBeDocumented
            @Target(AnnotationTarget.CLASS)           
            public annotation class KikType(val group: String, val version: String, val kind: String)
            
            @MustBeDocumented
            @Target(AnnotationTarget.CLASS)
            public annotation class KikTypePart
            
            @MustBeDocumented
            @Target(AnnotationTarget.PROPERTY)
            public annotation class KikProperty(val name: String, val required: Boolean = false)
            
            @MustBeDocumented
            @Target(AnnotationTarget.PROPERTY)
            public annotation class KikInline

            """.trimIndent()
    )

internal fun prepare(vararg sourceFiles: SourceFile): KotlinCompilation {
    return KotlinCompilation().apply {
        compilerPluginRegistrars = listOf(SerializationComponentRegistrar())
        commandLineProcessors = listOf(KikCommandLineProcessor())
        pluginOptions =
            listOf(
                PluginOption("org.erwinkok.kik.kik-compiler-plugin", "enabled", "true")
            )
        inheritClassPath = true
        sources = sourceFiles.asList() + annotations
        verbose = false
        languageVersion = "2.0"
    }
}
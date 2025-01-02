// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:OptIn(ExperimentalCompilerApi::class)

package org.erwinkok.kik.compiler

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

internal class TestPropertyChecker {
    @Test
    fun `type parameters`() {
        val companion = kotlin(
            "PropTypeParam.kt",
            """
            package org.erwinkok.kik.typesystem.compiler.test
            
            import org.erwinkok.kik.typesystem.KikProperty
            import org.erwinkok.kik.typesystem.KikType

            class X<T, U> 

            @KikType(group = "AGroup", version = "AVersion", kind = "AKind")
            class TestClass {
                @KikProperty(name = "C", required = true) 
                var C: X<String, Int>? = null
            }
            """.trimIndent()
        )
        val result = prepare(companion).compile()
        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "Property 'var C: X<String, Int>?' has type parameters, which is not supported")
    }

//    @Test
    fun `properties`() {
        val companion = kotlin(
            "PropTypeParam.kt",
            """
            package org.erwinkok.kik.typesystem.compiler.test
            
            import org.erwinkok.kik.typesystem.KikProperty
            import org.erwinkok.kik.typesystem.KikType

            @KikType(group = "AGroup", version = "AVersion", kind = "AKind")
            class TestClass(
                @KikProperty("P1") val P1: String,
                @KikProperty("P2") val P2: String = "something",
                @KikProperty("P3") val P3: Int?,
                @KikProperty("P4") val P4: Int? = 34,
            ) {
                @KikProperty("A")
                val A: String? = null

                @KikProperty(name = "B", required = true)
                val B: Int = 42
                                              
                var D: String = "blie"
            }
            """.trimIndent()
        )
        val result = prepare(companion).compile()
        assertEquals(ExitCode.OK, result.exitCode)
    }
}
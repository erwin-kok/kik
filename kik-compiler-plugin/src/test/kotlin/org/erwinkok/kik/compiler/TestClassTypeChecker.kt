@file:OptIn(ExperimentalCompilerApi::class)

package org.erwinkok.kik.compiler

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

internal class TestClassTypeChecker {
    @Test
    fun `classes can not be have super classes`() {
        val superClass = kotlin(
            "SuperClass.kt",
            """
            package org.erwinkok.kik.typesystem.compiler.test
            
            import org.erwinkok.kik.typesystem.KikType

            open class Super 

            @KikType(group = "AGroup", version = "AVersion", kind = "AKind")
            class TestClass: Super()
            """.trimIndent()
        )
        val result = prepare(superClass).compile()
        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "Class tagged with @KikType has one or more super classes/interfaces")
    }

    @Test
    fun `classes can not be have interfaces`() {
        val interfaces = kotlin(
            "Interface.kt",
            """
            package org.erwinkok.kik.typesystem.compiler.test
            
            import org.erwinkok.kik.typesystem.KikType

            interface IFace 

            @KikType(group = "AGroup", version = "AVersion", kind = "AKind")
            class TestClass: IFace
            """.trimIndent()
        )
        val result = prepare(interfaces).compile()
        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "Class tagged with @KikType has one or more super classes/interfaces")
    }

    @Test
    fun `objects can not be annotated with KikType`() {
        val objects = kotlin(
            "Object.kt",
            """
            package org.erwinkok.kik.typesystem.compiler.test
            
            import org.erwinkok.kik.typesystem.KikType
            
            @KikType(group = "AGroup", version = "AVersion", kind = "AKind")
            object TestObject
            """.trimIndent()
        )
        val result = prepare(objects).compile()
        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "Objects can not be annotated with @KikType.")
    }

    @Test
    fun `anonymous classes can not be annotated with KikType`() {
        val anonymous = kotlin(
            "Anonymous.kt",
            """
            package org.erwinkok.kik.typesystem.compiler.test
            
            import org.erwinkok.kik.typesystem.KikType
                       
            val a = @KikType(group = "AGroup", version = "AVersion", kind = "AKind") object: Any() {}
            """.trimIndent()
        )
        val result = prepare(anonymous).compile()
        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "Anonymous classes or contained in its classes can not be annotation with @KikType.")
    }

    @Test
    fun `inner classes can not be annotated with KikType`() {
        val inner = kotlin(
            "Inner.kt",
            """
            package org.erwinkok.kik.typesystem.compiler.test
            
            import org.erwinkok.kik.typesystem.KikType
                       
            class Test {
                @KikType(group = "AGroup", version = "AVersion", kind = "AKind") 
                inner class TestClass
            }
            """.trimIndent()
        )
        val result = prepare(inner).compile()
        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "Inner (with reference to outer this) classes cannot be annotated with @KikType.")
    }

    @Test
    fun `abstract classes can not be annotated with KikType`() {
        val inner = kotlin(
            "Abstract.kt",
            """
            package org.erwinkok.kik.typesystem.compiler.test
            
            import org.erwinkok.kik.typesystem.KikType
                
            @KikType(group = "AGroup", version = "AVersion", kind = "AKind")
            abstract class Test           
            """.trimIndent()
        )
        val result = prepare(inner).compile()
        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "Abstract classes cannot be annotated with @KikType.")
    }

    @Test
    fun `type parameters`() {
        val typeParameters = kotlin(
            "TypeParams.kt",
            """
            package org.erwinkok.kik.typesystem.compiler.test
            
            import org.erwinkok.kik.typesystem.KikType

            @KikType(group = "AGroup", version = "AVersion", kind = "AKind")
            class TestClass<P>
            """.trimIndent()
        )
        val result = prepare(typeParameters).compile()
        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "Class annotated with @KikType has one or more type parameters")
    }

    @Test
    fun `companion object`() {
        val companion = kotlin(
            "Companion.kt",
            """
            package org.erwinkok.kik.typesystem.compiler.test
            
            import org.erwinkok.kik.typesystem.KikType

            @KikType(group = "AGroup", version = "AVersion", kind = "AKind")
            class TestClass {
                companion object CompanionObject                               
            }
            """.trimIndent()
        )
        val result = prepare(companion).compile()
        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "Class annotated with @KikType has a companion object")
    }

    @Test
    fun `duplicate enum properties`() {
        val companion = kotlin(
            "DuplicateEnumProp.kt",
            """
            package org.erwinkok.kik.typesystem.compiler.test
            
            import org.erwinkok.kik.typesystem.KikProperty
           
            enum class TestEnum {
                @KikProperty("A")
                A,
                @KikProperty("A")
                B                               
            }
            """.trimIndent()
        )
        val result = prepare(companion).compile()
        assertEquals(ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(result.messages, "Enum class 'enum class TestEnum : Enum<TestEnum>' has duplicate property name 'A' in entry 'B'")
    }
}

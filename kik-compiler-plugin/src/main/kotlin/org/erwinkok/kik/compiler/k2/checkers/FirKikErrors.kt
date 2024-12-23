// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2.checkers

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.error3
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.psi.KtAnnotationEntry

internal object FirKikErrors : BaseDiagnosticRendererFactory() {
    val SUPERCLASS_NOT_SUPPORTED by error1<KtAnnotationEntry, String>()
    val OBJECTS_NOT_SUPPORTED by error0<KtAnnotationEntry>()
    val ANONYMOUS_OBJECTS_NOT_SUPPORTED by error0<KtAnnotationEntry>()
    val INNER_CLASSES_NOT_SUPPORTED by error0<KtAnnotationEntry>()
    val ABSTRACT_CLASSES_NOT_SUPPORTED by error0<KtAnnotationEntry>()
    val TYPE_PARAMETERS_NOT_SUPPORTED by error1<KtAnnotationEntry, String>()
    val COMPANION_OBJECT_NOT_SUPPORTED by error1<KtAnnotationEntry, String>()
    val DUPLICATE_PROPERTY_NAME_ENUM by error3<KtAnnotationEntry, FirClassSymbol<*>, String, String>()
    val DUPLICATE_PROPERTY_NAME by error1<KtAnnotationEntry, String>()
    val GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED by error0<PsiElement>()
    val PROPERTY_TYPE_PARAMETER_NOT_SUPPORTED by error1<KtAnnotationEntry, FirPropertySymbol>()

    override val MAP = KtDiagnosticFactoryToRendererMap("KikTypeSystem").apply {
        put(
            SUPERCLASS_NOT_SUPPORTED,
            "Class tagged with @KikType has one or more super classes/interfaces ''{0}'', which is not supported",
            CommonRenderers.STRING
        )
        put(
            OBJECTS_NOT_SUPPORTED,
            "Objects can not be annotated with @KikType."
        )
        put(
            ANONYMOUS_OBJECTS_NOT_SUPPORTED,
            "Anonymous classes or contained in its classes can not be annotation with @KikType."
        )
        put(
            INNER_CLASSES_NOT_SUPPORTED,
            "Inner (with reference to outer this) classes cannot be annotated with @KikType."
        )
        put(
            ABSTRACT_CLASSES_NOT_SUPPORTED,
            "Abstract classes cannot be annotated with @KikType."
        )
        put(
            TYPE_PARAMETERS_NOT_SUPPORTED,
            "Class annotated with @KikType has one or more type parameters ''{0}'', which is not supported",
            CommonRenderers.STRING
        )
        put(
            COMPANION_OBJECT_NOT_SUPPORTED,
            "Class annotated with @KikType has a companion object ''{0}'', which is not supported",
            CommonRenderers.STRING
        )
        put(
            DUPLICATE_PROPERTY_NAME_ENUM,
            "Enum class ''{0}'' has duplicate property name ''{1}'' in entry ''{2}''",
            FirDiagnosticRenderers.SYMBOL,
            CommonRenderers.STRING,
            CommonRenderers.STRING
        )
        put(
            DUPLICATE_PROPERTY_NAME,
            "KikType class has duplicate property name of property ''{0}''",
            CommonRenderers.STRING
        )
        put(
            GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED,
            "Serialization of Arrays with generic type arguments is impossible because of unknown compile-time type."
        )
        put(
            PROPERTY_TYPE_PARAMETER_NOT_SUPPORTED,
            "Property ''{0}'' has type parameters, which is not supported",
            FirDiagnosticRenderers.SYMBOL
        )
    }

    init {
        RootDiagnosticRendererFactory.registerFactory(this)
    }
}

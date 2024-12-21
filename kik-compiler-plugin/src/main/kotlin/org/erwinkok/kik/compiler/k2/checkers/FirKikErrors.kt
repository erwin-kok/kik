// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2.checkers

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtAnnotationEntry

internal object FirKikErrors : BaseDiagnosticRendererFactory() {
    val SUPERCLASS_NOT_SUPPORTED by error1<KtAnnotationEntry, String>()
    val OBJECTS_NOT_SUPPORTED by error0<PsiElement>()
    val ANONYMOUS_OBJECTS_NOT_SUPPORTED by error0<PsiElement>()
    val INNER_CLASSES_NOT_SUPPORTED by error0<PsiElement>()
    val TYPE_PARAMETERS_NOT_SUPPORTED by error1<KtAnnotationEntry, String>()

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
            "Inner (with reference to outer this) classes cannot be annotated with @KikType. Remove 'inner' keyword."
        )
        put(
            TYPE_PARAMETERS_NOT_SUPPORTED,
            "Class annotated with @KikType has one or more type parameters ''{0}'', which is not supported",
            CommonRenderers.STRING
        )
    }

    init {
        RootDiagnosticRendererFactory.registerFactory(this)
    }
}

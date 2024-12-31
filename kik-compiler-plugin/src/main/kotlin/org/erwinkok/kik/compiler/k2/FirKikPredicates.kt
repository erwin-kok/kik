package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.KikAnnotations
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate

internal object FirKikPredicates {
    internal val annotatedWithKikType = DeclarationPredicate.create {
        annotated(setOf(KikAnnotations.kikTypeAnnotationFqName))
    }
}

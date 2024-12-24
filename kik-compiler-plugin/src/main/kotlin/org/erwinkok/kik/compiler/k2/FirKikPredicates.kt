package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.resolve.KikAnnotations
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate

internal object FirKikPredicates {
    internal val annotatedWithKikType = DeclarationPredicate.create {
        annotated(setOf(KikAnnotations.kikTypeAnnotationFqName))
    }
}

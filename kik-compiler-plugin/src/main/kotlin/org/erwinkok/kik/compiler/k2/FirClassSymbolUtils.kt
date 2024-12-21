// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.resolve.KikAnnotations
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

internal fun FirClassSymbol<*>.hasKikAnnotation(session: FirSession): Boolean {
    return serializableAnnotationWithoutArgs(session) != null
}

internal fun FirBasedSymbol<*>.serializableAnnotationWithoutArgs(session: FirSession): FirAnnotation? {
    return resolvedCompilerAnnotationsWithClassIds.serializableAnnotation(session)
}

internal fun List<FirAnnotation>.serializableAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationByClassId(KikAnnotations.kikTypeAnnotationClassId, session)
}

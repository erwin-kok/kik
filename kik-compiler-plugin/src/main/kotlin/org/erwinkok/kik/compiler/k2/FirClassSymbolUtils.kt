// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.resolve.AnnotationParameterNames
import org.erwinkok.kik.compiler.resolve.KikAnnotations
import org.erwinkok.kik.compiler.resolve.KikAnnotations.kikPropertyAnnotationClassId
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getBooleanArgument
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType

internal fun FirClassSymbol<*>.hasKikAnnotation(session: FirSession): Boolean {
    return kikAnnotationWithoutArgs(session) != null
}

internal fun FirClassSymbol<*>.kikAnnotationSource(session: FirSession): KtSourceElement? {
    return kikAnnotationWithoutArgs(session)?.source
}

internal fun FirBasedSymbol<*>.kikAnnotationWithoutArgs(session: FirSession): FirAnnotation? {
    return resolvedCompilerAnnotationsWithClassIds.kikAnnotation(session)
}

internal fun List<FirAnnotation>.kikAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationByClassId(KikAnnotations.kikTypeAnnotationClassId, session)
}

internal fun FirBasedSymbol<*>.getPropertyNameValue(session: FirSession): String? =
    getKikPropertyNameAnnotation(session)?.getStringArgument(AnnotationParameterNames.NAME, session)

internal fun FirBasedSymbol<*>.getPropertyRequiredValue(session: FirSession): Boolean =
    getKikPropertyNameAnnotation(session)?.getBooleanArgument(AnnotationParameterNames.REQUIRED, session) == true

internal fun FirBasedSymbol<*>.getKikPropertyNameAnnotation(session: FirSession): FirAnnotation? =
    resolvedAnnotationsWithArguments.getAnnotationByClassId(kikPropertyAnnotationClassId, session)

internal fun FirBasedSymbol<*>.hasKikInline(session: FirSession): Boolean =
    hasAnnotation(KikAnnotations.kikInlineAnnotationClassId, session)

internal val ConeKotlinType.isTypeParameter: Boolean
    get() = this is ConeTypeParameterType

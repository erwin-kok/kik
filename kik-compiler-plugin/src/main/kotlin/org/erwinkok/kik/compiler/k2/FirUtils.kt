// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.AnnotationParameterNames
import org.erwinkok.kik.compiler.KikAnnotations
import org.erwinkok.kik.compiler.KikAnnotations.kikPropertyAnnotationClassId
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.isEnumClass
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getBooleanArgument
import org.jetbrains.kotlin.fir.declarations.getDeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.deserialization.toQualifiedPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind

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
    return getAnnotationByClassId(KikAnnotations.kikTypePartAnnotationClassId, session) ?: getAnnotationByClassId(KikAnnotations.kikTypeAnnotationClassId, session)
}

internal fun FirBasedSymbol<*>.getPropertyNameValue(session: FirSession): String? =
    getKikPropertyNameAnnotation(session)?.getStringArgument(AnnotationParameterNames.NAME, session)

internal fun FirBasedSymbol<*>.getPropertyRequiredValue(session: FirSession): Boolean =
    getKikPropertyNameAnnotation(session)?.getBooleanArgument(AnnotationParameterNames.REQUIRED, session) == true

internal fun FirBasedSymbol<*>.getKikPropertyNameAnnotation(session: FirSession): FirAnnotation? =
    resolvedAnnotationsWithArguments.getAnnotationByClassId(kikPropertyAnnotationClassId, session)

internal fun FirBasedSymbol<*>.hasKikInline(session: FirSession): Boolean =
    hasAnnotation(KikAnnotations.kikInlineAnnotationClassId, session)

internal fun FirClassSymbol<*>.isSerializableEnum(session: FirSession): Boolean {
    return classKind.isEnumClass && hasKikAnnotation(session)
}

internal fun FirClassSymbol<*>.shouldHaveGeneratedMethodsInCompanion(session: FirSession): Boolean =
    isSerializableEnum(session) || (classKind == ClassKind.CLASS && hasKikAnnotation(session))

internal val ConeKotlinType.isTypeParameter: Boolean
    get() = this is ConeTypeParameterType

internal fun FirClassLikeDeclaration.markAsDeprecatedHidden(session: FirSession) {
    replaceAnnotations(annotations + listOf(createDeprecatedHiddenAnnotation(session)))
    replaceDeprecationsProvider(this.getDeprecationsProvider(session))
}

private fun createDeprecatedHiddenAnnotation(session: FirSession): FirAnnotation = buildAnnotation {
    val deprecatedAnno =
        session.symbolProvider.getClassLikeSymbolByClassId(StandardClassIds.Annotations.Deprecated) as FirRegularClassSymbol

    annotationTypeRef = deprecatedAnno.defaultType().toFirResolvedTypeRef()

    argumentMapping = buildAnnotationArgumentMapping {
        mapping[Name.identifier("message")] = buildLiteralExpression(
            null,
            ConstantValueKind.String,
            "This synthesized declaration should not be used directly",
            setType = true
        )

        // It has nothing to do with enums deserialization, but it is simply easier to build it this way.
        mapping[Name.identifier("level")] = buildEnumEntryDeserializedAccessExpression {
            enumClassId = StandardClassIds.DeprecationLevel
            enumEntryName = Name.identifier("HIDDEN")
        }.toQualifiedPropertyAccessExpression(session)
    }
}

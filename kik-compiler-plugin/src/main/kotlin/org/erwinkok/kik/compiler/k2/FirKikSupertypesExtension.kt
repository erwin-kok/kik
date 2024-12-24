// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.k2.FirKikPredicates.annotatedWithKikType
import org.erwinkok.kik.compiler.resolve.KikEntityNames.kikCommonTypeClassId
import org.jetbrains.kotlin.descriptors.isClass
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType

internal class FirKikSupertypesExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {
    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        return isKikTypeClass(declaration)
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService
    ): List<ConeKotlinType> {
        return if (isKikTypeClass(classLikeDeclaration) && resolvedSupertypes.all { it.coneType.classId != kikCommonTypeClassId }) {
            listOf(kikCommonTypeClassId.constructClassLikeType(emptyArray(), false))
        } else {
            emptyList()
        }
    }

    private fun isKikTypeClass(declaration: FirClassLikeDeclaration): Boolean {
        return declaration is FirClass && declaration.classKind.isClass
                && session.predicateBasedProvider.matches(annotatedWithKikType, declaration)
    }
}

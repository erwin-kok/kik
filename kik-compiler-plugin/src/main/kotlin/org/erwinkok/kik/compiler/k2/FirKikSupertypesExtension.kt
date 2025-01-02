// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.KikEntityNames
import org.erwinkok.kik.compiler.KikPackages
import org.erwinkok.kik.compiler.k2.FirKikPredicates.annotatedWithKik
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.konan.isNative

internal class FirKikSupertypesExtension(session: FirSession) : FirSupertypeGenerationExtension(session) {
    private val isJvmOrMetadata = !session.moduleData.platform.run { isNative() || isJs() || isWasm() }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean =
        isSerializableObjectAndNeedsFactory(declaration) || isCompanionAndNeedsFactory(declaration)

    private fun isSerializableObjectAndNeedsFactory(declaration: FirClassLikeDeclaration): Boolean {
        if (isJvmOrMetadata) return false
        return declaration is FirClass && declaration.classKind.isObject
                && session.predicateBasedProvider.matches(annotatedWithKik, declaration)
    }

    private fun isCompanionAndNeedsFactory(declaration: FirClassLikeDeclaration): Boolean {
        if (isJvmOrMetadata) return false
        if (declaration !is FirRegularClass) return false
        if (!declaration.isCompanion) return false
        val parentSymbol = declaration.symbol.getContainingDeclaration(session) as FirClassSymbol<*>
        return session.predicateBasedProvider.matches(annotatedWithKik, parentSymbol)
                && parentSymbol.companionNeedsSerializerFactory(session)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(annotatedWithKik)
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService
    ): List<ConeKotlinType> {
        return when {
            isSerializableObjectAndNeedsFactory(classLikeDeclaration) || isCompanionAndNeedsFactory(classLikeDeclaration) -> {
                val serializerFactoryClassId = ClassId(
                    KikPackages.internalPackageFqName,
                    KikEntityNames.SERIALIZER_FACTORY_INTERFACE_NAME
                )
                if (resolvedSupertypes.any { it.coneType.classId == serializerFactoryClassId }) return emptyList()
                listOf(serializerFactoryClassId.constructClassLikeType(emptyArray(), false))
            }

            else -> emptyList()
        }
    }

}

// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.resolve.KikClassIds.generatedSerializerId
import org.erwinkok.kik.compiler.resolve.KikEntityNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class FirKikResolveExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        val result = mutableSetOf<Name>()
        if (classSymbol.shouldHaveGeneratedMethodsInCompanion(session)) {
            result += SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        }
        if (classSymbol.hasKikAnnotation(session)) {
            result += KikEntityNames.KIK_CLASS_NAME
        }
        return result
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        if (owner !is FirRegularClassSymbol) {
            return null
        }
        if (!session.predicateBasedProvider.matches(FirKikPredicates.annotatedWithKikType, owner)) {
            return null
        }
        val result = when (name) {
            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> generateCompanionDeclaration(owner)
            KikEntityNames.KIK_CLASS_NAME -> generateSerializerImplClass(owner)
            else -> error("Can't generate class ${owner.classId.createNestedClassId(name).asSingleFqName()}")
        }
        return result
    }

    private fun generateCompanionDeclaration(owner: FirRegularClassSymbol): FirRegularClassSymbol? {
        if (owner.companionObjectSymbol != null) return null
        val companion = createCompanionObject(owner, FirKikPluginKey)
        return companion.symbol
    }

    private fun generateSerializerImplClass(owner: FirRegularClassSymbol): FirClassLikeSymbol<*> {
        val hasTypeParams = owner.typeParameterSymbols.isNotEmpty()
        val serializerKind = if (hasTypeParams) ClassKind.CLASS else ClassKind.OBJECT
        val serializerFirClass = createNestedClass(owner, KikEntityNames.KIK_CLASS_NAME, FirKikPluginKey, serializerKind) {
            modality = Modality.FINAL

            for (parameter in owner.typeParameterSymbols) {
                typeParameter(parameter.name)
            }
            superType { typeParameters ->
                generatedSerializerId.constructClassLikeType(
                    arrayOf(
                        owner.constructType(
                            typeParameters.map { it.toConeType() }.toTypedArray(),
                        )
                    ),
                )
            }
        }.apply {
            markAsDeprecatedHidden(session)
        }
        return serializerFirClass.symbol
    }
}

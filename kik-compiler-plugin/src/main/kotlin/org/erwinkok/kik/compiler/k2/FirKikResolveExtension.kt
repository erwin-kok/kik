// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.KikClassIds.generatedSerializerId
import org.erwinkok.kik.compiler.KikEntityNames
import org.erwinkok.kik.compiler.KikEntityNames.OBJECT_REFERENCE
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.scopeForSupertype
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class FirKikResolveExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        val result = mutableSetOf<Name>()
        if (classSymbol.shouldHaveGeneratedMethodsInCompanion(session)) {
            result += SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        }
        if (classSymbol.hasKikAnnotation(session)) {
            result += KikEntityNames.SERIALIZER_CLASS_NAME
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
            KikEntityNames.SERIALIZER_CLASS_NAME -> generateSerializerImplClass(owner)
            else -> error("Can't generate class ${owner.classId.createNestedClassId(name).asSingleFqName()}")
        }
        return result
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val result = mutableSetOf<Name>()
        result += OBJECT_REFERENCE
        return result
    }

//    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
//        val owner = context?.owner ?: return emptyList()
//        val target = getFromSupertype(callableId, owner) { it.getProperties(callableId.callableName).filterIsInstance<FirPropertySymbol>() }
//        val property = createMemberProperty(
//            owner,
//            KikPluginKey,
//            callableId.callableName,
//            target.resolvedReturnType
//        )
//        return listOf(property.symbol)
//    }

    @OptIn(SymbolInternals::class)
    private fun <T> getFromSupertype(callableId: CallableId, owner: FirClassSymbol<*>, extractor: (FirTypeScope) -> List<T>): T {
        val scopeSession = ScopeSession()
        val scopes = lookupSuperTypes(
            owner, lookupInterfaces = true, deep = false, useSiteSession = session
        ).mapNotNull { useSiteSuperType ->
            useSiteSuperType.scopeForSupertype(session, scopeSession, owner.fir, memberRequiredPhase = null)
        }
        val targets = scopes.flatMap { extractor(it) }
        return targets.singleOrNull() ?: error("Zero or multiple overrides found for ${callableId.callableName} in $owner --- ${targets.joinToString(", ")}")
    }

    private fun generateCompanionDeclaration(owner: FirRegularClassSymbol): FirRegularClassSymbol? {
        if (owner.companionObjectSymbol != null) return null
        val companion = createCompanionObject(owner, KikPluginKey)
        return companion.symbol
    }

    private fun generateSerializerImplClass(owner: FirRegularClassSymbol): FirClassLikeSymbol<*> {
        val hasTypeParams = owner.typeParameterSymbols.isNotEmpty()
        val serializerKind = if (hasTypeParams) ClassKind.CLASS else ClassKind.OBJECT
        val serializerFirClass = createNestedClass(owner, KikEntityNames.SERIALIZER_CLASS_NAME, KikPluginKey, serializerKind) {
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

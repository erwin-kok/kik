// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
@file:OptIn(SymbolInternals::class)

package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.KikClassIds.generatedSerializerId
import org.erwinkok.kik.compiler.KikClassIds.kSerializerId
import org.erwinkok.kik.compiler.KikEntityNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities.Internal
import org.jetbrains.kotlin.descriptors.Visibilities.Public
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.scopeForSupertype
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class FirKikResolveExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val SERIALIZER_FUNCTION_NAMES = setOf(
        SpecialNames.INIT,
        KikEntityNames.SAVE_NAME,
        KikEntityNames.LOAD_NAME,
        KikEntityNames.SERIAL_DESC_FIELD_NAME
    )

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(FirKikPredicates.annotatedWithKik)
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        val result = mutableSetOf<Name>()
        if (classSymbol.isSerializable(session)) {
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
        if (!session.predicateBasedProvider.matches(FirKikPredicates.annotatedWithKik, owner)) {
            return null
        }
        val result = when (name) {
            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> generateCompanionDeclaration(owner)
            KikEntityNames.SERIALIZER_CLASS_NAME -> generateSerializerImplementationClass(owner)
            else -> error("Can't generate class ${owner.classId.createNestedClassId(name).asSingleFqName()}")
        }
        return result
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val result = mutableSetOf<Name>()
        if (classSymbol.isCompanion) {
            result += KikEntityNames.SERIALIZER_PROVIDER_NAME
            val origin = classSymbol.origin as? FirDeclarationOrigin.Plugin
            if (origin?.key == KikPluginKey) {
                result += SpecialNames.INIT
            }
        } else if (classSymbol.classId.shortClassName == KikEntityNames.SERIALIZER_CLASS_NAME) {
            result += SERIALIZER_FUNCTION_NAMES
            if (classSymbol.resolvedSuperTypes.any { it.classId == generatedSerializerId }) {
                result += KikEntityNames.CHILD_SERIALIZERS_GETTER
            }
        }
        return result
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        if (owner.name != KikEntityNames.SERIALIZER_CLASS_NAME) {
            return emptyList()
        }

        return when (callableId.callableName) {
            KikEntityNames.SERIAL_DESC_FIELD_NAME -> {
                val target = getFromSupertype(owner, callableId.callableName) {
                    it
                        .getProperties(callableId.callableName)
                        .filterIsInstance<FirPropertySymbol>()
                }
                val property = createMemberProperty(
                    owner,
                    KikPluginKey,
                    callableId.callableName,
                    target.resolvedReturnType
                )
                listOf(property.symbol)
            }

            else -> emptyList()
        }
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val result = mutableListOf<FirConstructorSymbol>()
        result += createDefaultPrivateConstructor(context.owner, KikPluginKey).symbol
        return result
    }

    @OptIn(SymbolInternals::class)
    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        val containingSymbol = owner.getContainingDeclaration(session) as? FirClassSymbol<*>
        return if (containingSymbol != null && owner.isCompanion && containingSymbol.isSerializable(session)) {
            when (callableId.callableName) {
                KikEntityNames.SERIALIZER_PROVIDER_NAME -> {
                    generateSerializerFunction(owner, containingSymbol)
                }

                KikEntityNames.GENERATED_SERIALIZER_PROVIDER_NAME -> {
                    generatedSerializer(owner, containingSymbol)
                }

                else -> emptyList()
            }
        } else if (owner.name == KikEntityNames.SERIALIZER_CLASS_NAME) {
            when (callableId.callableName) {
                SpecialNames.INIT,
                KikEntityNames.SAVE_NAME,
                KikEntityNames.LOAD_NAME,
                KikEntityNames.CHILD_SERIALIZERS_GETTER,
                KikEntityNames.TYPE_PARAMS_SERIALIZERS_GETTER -> {
                    generateFunction(owner, callableId)
                }

                else -> emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun generateCompanionDeclaration(owner: FirRegularClassSymbol): FirRegularClassSymbol? {
        if (owner.companionObjectSymbol != null) {
            return null
        }
        val companion = createCompanionObject(owner, KikPluginKey)
        return companion.symbol
    }

    private fun generateSerializerImplementationClass(owner: FirRegularClassSymbol): FirClassLikeSymbol<*> {
        val serializerFirClass = createNestedClass(
            owner,
            KikEntityNames.SERIALIZER_CLASS_NAME,
            KikPluginKey,
            ClassKind.OBJECT
        ) {
            modality = Modality.FINAL
            superType { typeParameters ->
                generatedSerializerId.constructClassLikeType(arrayOf(owner.constructType(emptyArray())))
            }
        }
        serializerFirClass.markAsDeprecatedHidden(session)
        return serializerFirClass.symbol
    }

    @OptIn(SymbolInternals::class)
    private fun <T> getFromSupertype(owner: FirClassSymbol<*>, name: Name, extractor: (FirTypeScope) -> List<T>): T {
        val scopeSession = ScopeSession()
        val scopes = lookupSuperTypes(
            owner,
            lookupInterfaces = true,
            deep = false,
            useSiteSession = session
        ).mapNotNull { useSiteSuperType ->
            useSiteSuperType.scopeForSupertype(session, scopeSession, owner.fir, memberRequiredPhase = null)
        }
        val targets = scopes.flatMap { extractor(it) }
        return targets.singleOrNull() ?: error("Zero or multiple overrides found for $name in $owner --- ${targets.joinToString(", ")}")
    }

    private fun generateSerializerFunction(owner: FirClassSymbol<*>, containingSymbol: FirClassSymbol<*>): List<FirNamedFunctionSymbol> {
        val function = createMemberFunction(
            owner,
            KikPluginKey,
            KikEntityNames.SERIALIZER_PROVIDER_NAME,
            returnTypeProvider = { typeParameters ->
                kSerializerId.constructClassLikeType(
                    arrayOf(containingSymbol.constructType(emptyArray())),
                )
            }
        ) {
            visibility = Public
        }
        return listOf(function.symbol)
    }

    private fun generatedSerializer(owner: FirClassSymbol<*>, containingSymbol: FirClassSymbol<*>): List<FirNamedFunctionSymbol> {
        val function = createMemberFunction(
            owner,
            KikPluginKey,
            KikEntityNames.GENERATED_SERIALIZER_PROVIDER_NAME,
            returnTypeProvider = { _ ->
                kSerializerId.constructClassLikeType(arrayOf(containingSymbol.constructType(emptyArray())))
            }
        ) {
            visibility = Internal
        }
        return listOf(function.symbol)
    }

    @OptIn(SymbolInternals::class)
    private fun generateFunction(owner: FirClassSymbol<*>, callableId: CallableId): List<FirNamedFunctionSymbol> {
        val target = getFromSupertype(owner, callableId.callableName, { it.getFunctions(callableId.callableName) })
        val original = target.fir
        val copy = buildSimpleFunctionCopy(original) {
            symbol = FirNamedFunctionSymbol(callableId)
            origin = KikPluginKey.origin
            status = original.status.copy(modality = Modality.FINAL)
        }
        return listOf(copy.symbol)
    }
}

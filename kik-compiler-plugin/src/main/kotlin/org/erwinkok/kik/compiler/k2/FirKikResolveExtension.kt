// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.KikClassIds.generatedSerializerId
import org.erwinkok.kik.compiler.KikClassIds.kSerializerId
import org.erwinkok.kik.compiler.KikEntityNames
import org.erwinkok.kik.compiler.KikPackages
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createConstructor
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
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.scopeForSupertype
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class FirKikResolveExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(FirKikPredicates.annotatedWithKik)
    }

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
        if (!session.predicateBasedProvider.matches(FirKikPredicates.annotatedWithKik, owner)) {
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
        if (classSymbol.isCompanion) {
            result += KikEntityNames.SERIALIZER_PROVIDER_NAME
            val origin = classSymbol.origin as? FirDeclarationOrigin.Plugin
            if (origin?.key == KikPluginKey) {
                result += SpecialNames.INIT
            }
        } else if (classSymbol.classId.shortClassName == KikEntityNames.SERIALIZER_CLASS_NAME) {
            // TODO: check classSymbol for already added functions
            result += setOf(
                SpecialNames.INIT,
                KikEntityNames.SAVE_NAME,
                KikEntityNames.LOAD_NAME,
                KikEntityNames.SERIAL_DESC_FIELD_NAME
            )
            if (classSymbol.resolvedSuperTypes.any { it.classId == generatedSerializerId }) {
                result += KikEntityNames.CHILD_SERIALIZERS_GETTER

                if (classSymbol.typeParameterSymbols.isNotEmpty()) {
                    result += KikEntityNames.TYPE_PARAMS_SERIALIZERS_GETTER
                }
            }
        }
        return result
    }

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        if (!owner.isSerializer) return emptyList()
        if (callableId.callableName != KikEntityNames.SERIAL_DESC_FIELD_NAME) return emptyList()

        val target = getFromSupertype(callableId, owner) { it.getProperties(callableId.callableName).filterIsInstance<FirPropertySymbol>() }
        val property = createMemberProperty(
            owner,
            KikPluginKey,
            callableId.callableName,
            target.resolvedReturnType
        )
        return listOf(property.symbol)
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val owner = context.owner

        val result = mutableListOf<FirConstructorSymbol>()
        result += createDefaultPrivateConstructor(owner, KikPluginKey).symbol
        if (owner.name == KikEntityNames.SERIALIZER_CLASS_NAME && owner.typeParameterSymbols.isNotEmpty()) {
            result += createConstructor(owner, KikPluginKey) {
                visibility = Visibilities.Public
                owner.typeParameterSymbols.forEachIndexed { i, typeParam ->
                    valueParameter(
                        name = Name.identifier("${KikEntityNames.typeArgPrefix}$i"),
                        type = kSerializerId.constructClassLikeType(arrayOf(typeParam.toConeType()), false)
                    )
                }
            }.also {
                it.containingClassForStaticMemberAttr = owner.toLookupTag()
            }.symbol
        }
        return result
    }

    @OptIn(SymbolInternals::class)
    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        if (callableId.callableName == KikEntityNames.SERIALIZER_PROVIDER_NAME) {
            val containingSymbol = owner.getContainingDeclaration(session) as? FirClassSymbol<*>
            if (containingSymbol != null && owner.isCompanion && containingSymbol.shouldHaveGeneratedMethodsInCompanion(session)) {
                val serializableGetterInCompanion = generateSerializerGetterInCompanion(
                    owner,
                    containingSymbol,
                    callableId,
                    true
                )
                val serializableGetterFromFactory = if (containingSymbol.companionNeedsSerializerFactory(session)) {
                    val original = getFromSupertype(callableId, owner) { it.getFunctions(callableId.callableName) }.fir
                    generateSerializerFactoryVararg(owner, callableId, original)
                } else {
                    null
                }
                return listOfNotNull(serializableGetterInCompanion, serializableGetterFromFactory)
            } else {
                return emptyList()
            }
        } else if (callableId.callableName == KikEntityNames.GENERATED_SERIALIZER_PROVIDER_NAME) {
            val containingSymbol = owner.getContainingDeclaration(session) as? FirClassSymbol<*>
            if (containingSymbol != null && owner.isCompanion && containingSymbol.shouldHaveGeneratedMethodsInCompanion(session)) {
                val serializableGetterInCompanion = generateSerializerGetterInCompanion(
                    owner,
                    containingSymbol,
                    callableId,
                    false
                )
                return listOfNotNull(serializableGetterInCompanion)
            } else {
                return emptyList()
            }
        }
        if (!owner.isSerializer) {
            return emptyList()
        }
        val functionSet = setOf(
            SpecialNames.INIT,
            KikEntityNames.SAVE_NAME,
            KikEntityNames.LOAD_NAME,
            KikEntityNames.CHILD_SERIALIZERS_GETTER,
            KikEntityNames.TYPE_PARAMS_SERIALIZERS_GETTER
        )
        if (callableId.callableName !in functionSet) {
            return emptyList()
        }
        val target = getFromSupertype(callableId, owner) { it.getFunctions(callableId.callableName) }
        val original = target.fir
        val copy = buildSimpleFunctionCopy(original) {
            symbol = FirNamedFunctionSymbol(callableId)
            origin = KikPluginKey.origin
            status = original.status.copy(modality = Modality.FINAL)
        }
        return listOf(copy.symbol)
    }

    private fun generateCompanionDeclaration(owner: FirRegularClassSymbol): FirRegularClassSymbol? {
        if (owner.companionObjectSymbol != null) return null
        val companion = createCompanionObject(owner, KikPluginKey) {
            if (owner.companionNeedsSerializerFactory(session)) {
                val serializerFactoryClassId = ClassId(KikPackages.internalPackageFqName, KikEntityNames.SERIALIZER_FACTORY_INTERFACE_NAME)
                superType(serializerFactoryClassId.constructClassLikeType(emptyArray(), false))
            }
        }
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

    @OptIn(SymbolInternals::class)
    private fun <T> getFromSupertype(callableId: CallableId, owner: FirClassSymbol<*>, extractor: (FirTypeScope) -> List<T>): T {
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
        return targets.singleOrNull() ?: error("Zero or multiple overrides found for ${callableId.callableName} in $owner --- ${targets.joinToString(", ")}")
    }

    private fun generateSerializerGetterInCompanion(
        owner: FirClassSymbol<*>,
        serializableClassSymbol: FirClassSymbol<*>,
        callableId: CallableId,
        isPublic: Boolean
    ): FirNamedFunctionSymbol {
        val function = createMemberFunction(
            owner,
            KikPluginKey,
            callableId.callableName,
            returnTypeProvider = { typeParameters ->
                val parametersAsArguments = typeParameters.map { it.toConeType() }.toTypedArray<ConeTypeProjection>()
                kSerializerId.constructClassLikeType(
                    arrayOf(serializableClassSymbol.constructType(parametersAsArguments)),
                )
            }
        ) {
            serializableClassSymbol.typeParameterSymbols.forEachIndexed { i, typeParameterSymbol ->
                typeParameter(typeParameterSymbol.name)
                valueParameter(
                    Name.identifier("${KikEntityNames.typeArgPrefix}$i"),
                    { typeParameters ->
                        kSerializerId.constructClassLikeType(arrayOf(typeParameters[i].toConeType()), false)
                    }
                )
            }

            visibility = if (isPublic) Visibilities.Public else Visibilities.Internal
        }
        return function.symbol
    }

    private fun generateSerializerFactoryVararg(
        owner: FirClassSymbol<*>,
        callableId: CallableId,
        original: FirSimpleFunction
    ): FirNamedFunctionSymbol =
        createMemberFunction(owner, KikPluginKey, callableId.callableName, original.returnTypeRef.coneType) {
            val vpo = original.valueParameters.single()
            valueParameter(vpo.name, vpo.returnTypeRef.coneType, vpo.isCrossinline, vpo.isNoinline, vpo.isVararg, vpo.defaultValue != null)
        }.symbol

    private val FirClassSymbol<*>.isSerializer: Boolean
        get() = name == KikEntityNames.SERIALIZER_CLASS_NAME
}

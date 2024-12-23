// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2.services

import org.erwinkok.kik.compiler.k2.FirKikProperties
import org.erwinkok.kik.compiler.k2.FirKikProperty
import org.erwinkok.kik.compiler.k2.getPropertyNameValue
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.primaryConstructorSymbol
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.utils.addIfNotNull

internal class FirKikPropertiesProvider(session: FirSession) : FirExtensionSessionComponent(session) {
    private val cache: FirCache<FirClassSymbol<*>, FirKikProperties, Nothing?> =
        session.firCachesFactory.createCache(this::createKikProperties)

    fun getKikPropertiesForClass(classSymbol: FirClassSymbol<*>): FirKikProperties {
        return cache.getValue(classSymbol)
    }

    private fun createKikProperties(classSymbol: FirClassSymbol<*>): FirKikProperties {
        val allPropertySymbols = buildList {
            classSymbol
                .declaredMemberScope(session, memberRequiredPhase = null)
                .processAllProperties {
                    addIfNotNull(it as? FirPropertySymbol)
                }
        }
        val primaryConstructorProperties = allPropertySymbols.mapNotNull {
            val parameterSymbol = it.correspondingValueParameterFromPrimaryConstructor ?: return@mapNotNull null
            it to parameterSymbol.hasDefaultValue
        }.toMap().withDefault { false }

        val kikProperties = allPropertySymbols
            .filter { it.getPropertyNameValue(session) != null }
            .map {
                FirKikProperty(
                    session,
                    it,
                    it.hasInitializer
                )
            }
            .filterNot { it.transient }
            .partition { it.propertySymbol in primaryConstructorProperties }
            .let { (fromConstructor, standalone) ->
                buildList {
                    addAll(fromConstructor)
                    addAll(standalone)
                }
            }
        val isExternallySerializable = classSymbol.isEnumClass ||
                primaryConstructorProperties.size == (classSymbol.primaryConstructorSymbol(session)?.valueParameterSymbols?.size ?: 0)

        val (kikConstructorProperties, kikStandaloneProperties) = kikProperties.partition { it.propertySymbol in primaryConstructorProperties }

        return FirKikProperties(kikProperties, isExternallySerializable, kikConstructorProperties, kikStandaloneProperties)
    }
}

internal val FirSession.kikPropertiesProvider: FirKikPropertiesProvider by FirSession.sessionComponentAccessor()

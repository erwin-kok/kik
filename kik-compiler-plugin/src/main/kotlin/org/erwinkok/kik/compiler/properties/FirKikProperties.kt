// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.properties

import org.erwinkok.kik.compiler.k2.getPropertyNameValue
import org.erwinkok.kik.compiler.k2.getPropertyRequiredValue
import org.erwinkok.kik.compiler.k2.hasKikInline
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.deserialization.registeredInSerializationPluginMetadataExtension
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.utils.addIfNotNull

internal class FirKikProperty(
    session: FirSession,
    val propertySymbol: FirPropertySymbol,
    override val isConstructorParameterWithDefault: Boolean,
    declaresDefaultValue: Boolean
) : IKikProperty {
    override val name = propertySymbol.getPropertyNameValue(session) ?: propertySymbol.name.asString()
    override val originalDescriptorName = propertySymbol.name
    override val optional = !propertySymbol.getPropertyRequiredValue(session) && declaresDefaultValue
    override val inline = propertySymbol.hasKikInline(session)
}

internal class FirKikProperties(
    override val kikProperties: List<FirKikProperty>,
    override val kikConstructorProperties: List<FirKikProperty>,
    override val kikStandaloneProperties: List<FirKikProperty>,
) : IKikProperties<FirKikProperty> {
    companion object {
        fun fromClassSymbol(session: FirSession, classSymbol: FirClassSymbol<*>): FirKikProperties {
            val allPropertySymbols = buildPropertySymbols(session, classSymbol)
            val primaryConstructorProperties = buildPrimaryConstructorProperties(allPropertySymbols)
            val (kikConstructorProperties, kikStandaloneProperties) = allPropertySymbols
                .filter { isPropertySerializable(session, it) }
                .map { propertySymbol ->
                    val isConstructorParameterWithDefault = primaryConstructorProperties.getValue(propertySymbol)
                    val declaresDefaultValue = propertySymbol.hasInitializer
                    FirKikProperty(
                        session,
                        propertySymbol,
                        isConstructorParameterWithDefault,
                        declaresDefaultValue
                    )
                }
                .partition { it.propertySymbol in primaryConstructorProperties }
            val kikProperties = kikConstructorProperties + kikStandaloneProperties
            return FirKikProperties(kikProperties, kikConstructorProperties, kikStandaloneProperties)
        }

        private fun buildPropertySymbols(session: FirSession, classSymbol: FirClassSymbol<*>): List<FirPropertySymbol> = buildList {
            classSymbol
                .declaredMemberScope(session, memberRequiredPhase = null)
                .processAllProperties {
                    addIfNotNull(it as? FirPropertySymbol)
                }
        }

        private fun buildPrimaryConstructorProperties(allPropertySymbols: List<FirPropertySymbol>): Map<FirPropertySymbol, Boolean> {
            val result = mutableMapOf<FirPropertySymbol, Boolean>()
            for (propertySymbol in allPropertySymbols) {
                val parameterSymbol = propertySymbol.correspondingValueParameterFromPrimaryConstructor
                if (parameterSymbol != null) {
                    result[propertySymbol] = parameterSymbol.hasDefaultValue
                }
            }
            return result.withDefault { false }
        }

        private fun isPropertySerializable(session: FirSession, propertySymbol: FirPropertySymbol): Boolean {
            if (propertySymbol.getPropertyNameValue(session) == null) {
                return false
            }
            val hasBackingField = when (propertySymbol.origin) {
                FirDeclarationOrigin.Library -> propertySymbol.registeredInSerializationPluginMetadataExtension
                else -> propertySymbol.hasBackingField
            }
            return hasBackingField
        }
    }
}

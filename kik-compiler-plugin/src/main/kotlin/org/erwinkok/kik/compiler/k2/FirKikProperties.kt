// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.deserialization.registeredInSerializationPluginMetadataExtension
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

internal class FirKikProperties(
    val kikProperties: List<FirKikProperty>,
    val isExternallySerializable: Boolean,
    val kikConstructorProperties: List<FirKikProperty>,
    val kikStandaloneProperties: List<FirKikProperty>,
)

internal class FirKikProperty(
    session: FirSession,
    val propertySymbol: FirPropertySymbol,
    declaresDefaultValue: Boolean
) {
    val name = propertySymbol.getPropertyNameValue(session) ?: propertySymbol.name.asString()
    val optional = !propertySymbol.getPropertyRequiredValue(session) && declaresDefaultValue
    val inline = propertySymbol.hasKikInline(session)
    val transient: Boolean = run {
        if (propertySymbol.getPropertyNameValue(session) == null) return@run true
        val hasBackingField = when (propertySymbol.origin) {
            FirDeclarationOrigin.Library -> propertySymbol.registeredInSerializationPluginMetadataExtension
            else -> propertySymbol.hasBackingField
        }
        !hasBackingField
    }
}

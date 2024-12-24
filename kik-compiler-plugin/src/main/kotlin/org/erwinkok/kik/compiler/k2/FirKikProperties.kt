// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.resolve.IKikProperties
import org.erwinkok.kik.compiler.resolve.IKikProperty
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.deserialization.registeredInSerializationPluginMetadataExtension
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.Name

internal class FirKikProperties(
    override val kikProperties: List<FirKikProperty>,
    override val isExternallySerializable: Boolean,
    override val kikConstructorProperties: List<FirKikProperty>,
    override val kikStandaloneProperties: List<FirKikProperty>,
) : IKikProperties<FirKikProperty>

internal class FirKikProperty(
    session: FirSession,
    val propertySymbol: FirPropertySymbol,
    declaresDefaultValue: Boolean
) : IKikProperty {
    override val name = propertySymbol.getPropertyNameValue(session) ?: propertySymbol.name.asString()
    override val originalDescriptorName  = propertySymbol.name
    override val optional = !propertySymbol.getPropertyRequiredValue(session) && declaresDefaultValue
    override val inline = propertySymbol.hasKikInline(session)
    override val transient: Boolean = run {
        if (propertySymbol.getPropertyNameValue(session) == null) return@run true
        val hasBackingField = when (propertySymbol.origin) {
            FirDeclarationOrigin.Library -> propertySymbol.registeredInSerializationPluginMetadataExtension
            else -> propertySymbol.hasBackingField
        }
        !hasBackingField
    }
}

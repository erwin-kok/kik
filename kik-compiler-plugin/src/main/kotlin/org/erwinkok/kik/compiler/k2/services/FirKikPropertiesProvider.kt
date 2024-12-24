// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2.services

import org.erwinkok.kik.compiler.properties.FirKikProperties
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

internal class FirKikPropertiesProvider(session: FirSession) : FirExtensionSessionComponent(session) {
    private val cache: FirCache<FirClassSymbol<*>, FirKikProperties, Nothing?> =
        session.firCachesFactory.createCache { FirKikProperties.fromClassSymbol(this.session, it) }

    fun getKikPropertiesForClass(classSymbol: FirClassSymbol<*>): FirKikProperties {
        return cache.getValue(classSymbol)
    }
}

internal val FirSession.kikPropertiesProvider: FirKikPropertiesProvider by FirSession.sessionComponentAccessor()

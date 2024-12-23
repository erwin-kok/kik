// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2

import org.erwinkok.kik.compiler.k2.checkers.FirKikCheckersComponent
import org.erwinkok.kik.compiler.k2.services.FirKikPropertiesProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

internal class FirKikExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::FirKikCheckersComponent

        // services
        +::FirKikPropertiesProvider
    }
}

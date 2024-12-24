// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.resolve

import org.jetbrains.kotlin.name.Name

internal interface IKikProperty {
    val name: String
    val originalDescriptorName: Name
    val optional: Boolean
    val inline: Boolean
    val transient: Boolean
}

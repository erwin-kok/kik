// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.properties

import org.jetbrains.kotlin.name.Name

internal interface IKikProperty {
    val name: String
    val originalDescriptorName: Name
    val isConstructorParameterWithDefault: Boolean
    val optional: Boolean
    val inline: Boolean
}

internal fun List<IKikProperty>.bitMaskSlotCount(): Int = size / 32 + 1

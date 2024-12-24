// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.resolve

internal interface IKikProperties<S : IKikProperty> {
    val kikProperties: List<S>
    val isExternallySerializable: Boolean
    val kikConstructorProperties: List<S>
    val kikStandaloneProperties: List<S>
}

internal fun List<IKikProperty>.bitMaskSlotCount(): Int = size / 32 + 1

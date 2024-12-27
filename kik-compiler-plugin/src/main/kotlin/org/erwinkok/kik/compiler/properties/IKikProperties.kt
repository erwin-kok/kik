// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.properties

internal interface IKikProperties<S : IKikProperty> {
    val kikProperties: List<S>
    val kikConstructorProperties: List<S>
    val kikStandaloneProperties: List<S>
}

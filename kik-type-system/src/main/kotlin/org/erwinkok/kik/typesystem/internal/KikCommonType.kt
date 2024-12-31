// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.typesystem.internal

interface KikCommonType<T> {
    var kind: RealmObjectReference<out BaseRealmObject>?

    val xxx: T
}

package org.erwinkok.kik.typesystem

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
public annotation class KikType(val group: String, val version: String, val kind: String)

package org.erwinkok.kik.typesystem

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
public annotation class KikType(val group: String, val version: String, val kind: String)

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
public annotation class KikTypePart

@MustBeDocumented
@Target(AnnotationTarget.PROPERTY)
public annotation class KikProperty(val name: String, val required: Boolean = false)

@MustBeDocumented
@Target(AnnotationTarget.PROPERTY)
public annotation class KikInline

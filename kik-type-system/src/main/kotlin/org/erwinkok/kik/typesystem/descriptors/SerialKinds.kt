package org.erwinkok.kik.typesystem.descriptors

public sealed class SerialKind {
    public object ENUM : SerialKind()

    override fun toString(): String {
        // KNPE should never happen, because SerialKind is sealed and all inheritors are non-anonymous
        return this::class.simpleName!!
    }

    // Provide a stable hashcode for objects
    override fun hashCode(): Int = toString().hashCode()
}

public sealed class PrimitiveKind : SerialKind() {
    public object BOOLEAN : PrimitiveKind()
    public object BYTE : PrimitiveKind()
    public object CHAR : PrimitiveKind()
    public object SHORT : PrimitiveKind()
    public object INT : PrimitiveKind()
    public object LONG : PrimitiveKind()
    public object FLOAT : PrimitiveKind()
    public object DOUBLE : PrimitiveKind()
    public object STRING : PrimitiveKind()
}

public sealed class StructureKind : SerialKind() {
    public object CLASS : StructureKind()
    public object LIST : StructureKind()
    public object MAP : StructureKind()
    public object OBJECT : StructureKind()
}

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.erwinkok.kik.compiler.k1

import org.erwinkok.kik.compiler.KikClassIds.enumSerializerId
import org.erwinkok.kik.compiler.KikClassIds.referenceArraySerializerId
import org.erwinkok.kik.compiler.KikEntityNames
import org.erwinkok.kik.compiler.KikPackages
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isByte
import org.jetbrains.kotlin.ir.types.isChar
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.types.isShort
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.Name

internal class SerializableCompanionIrGenerator(
    val irClass: IrClass,
    val serializableIrClass: IrClass,
    compilerContext: KikPluginContext
) : IrBuilder(compilerContext) {
    fun generate() {
        val serializerGetterFunction =
            getSerializerGetterFunction(serializableIrClass, KikEntityNames.SERIALIZER_PROVIDER_NAME)?.takeIf {
                it.isFromPlugin()
            }
                ?: throw IllegalStateException(
                    "Can't find synthesized 'Companion.serializer()' function to generate, " +
                            "probably clash with user-defined function has occurred"
                )

        val serializer = requireNotNull(findTypeSerializer(compilerContext, serializableIrClass.defaultType))
        generateSerializerGetter(serializer, serializerGetterFunction)
    }

    private fun generateSerializerGetter(serializer: IrClassSymbol, methodDescriptor: IrSimpleFunction) {
        addFunctionBody(methodDescriptor) { getter ->
            val args: List<IrExpression> = getter.valueParameters.map { irGet(it) }
            val expr = serializerInstance(serializer, compilerContext, serializableIrClass.defaultType) { it, _ -> args[it] }
            +irReturn(requireNotNull(expr))
        }
    }

    fun IrBuilderWithScope.serializerInstance(
        serializerClassOriginal: IrClassSymbol?,
        pluginContext: KikPluginContext,
        kType: IrType,
        genericIndex: Int? = null,
        rootSerializableClass: IrClass? = null,
        genericGetter: ((Int, IrType) -> IrExpression)? = null,
    ): IrExpression? {
        return irNull()
    }

    private fun getSerializerGetterFunction(serializableIrClass: IrClass, name: Name): IrSimpleFunction? {
        val irClass = serializableIrClass.companionObject() ?: return null
        return irClass.findDeclaration<IrSimpleFunction> {
            it.name == name
                    && it.valueParameters.size == serializableIrClass.typeParameters.size
                    && it.valueParameters.all { p -> p.type.isKSerializer() }
                    && it.returnType.isKSerializer()
        }
    }

    fun findTypeSerializer(context: KikPluginContext, type: IrType): IrClassSymbol? {
        if (type.isTypeParameter()) return null
        if (type.isArray()) return context.referenceClassId(referenceArraySerializerId)
        val stdSer = findStandardKotlinTypeSerializer(context, type) // see if there is a standard serializer
            ?: findEnumTypeSerializer(context, type)
        if (stdSer != null) return stdSer
        return type.classOrNull?.owner.classSerializer() // check for serializer defined on the type
    }

    fun findEnumTypeSerializer(context: KikPluginContext, type: IrType): IrClassSymbol? {
        val classSymbol = type.classOrNull?.owner ?: return null

        // in any case, the function returns the serializer for the enum
        if (classSymbol.kind != ClassKind.ENUM_CLASS) return null

        return context.referenceClassId(enumSerializerId)
    }

    fun findStandardKotlinTypeSerializer(context: KikPluginContext, type: IrType): IrClassSymbol? {
        val typeName = type.classFqName?.toString()
        val name = when (typeName) {
            "Z" -> if (type.isBoolean()) "BooleanSerializer" else null
            "B" -> if (type.isByte()) "ByteSerializer" else null
            "S" -> if (type.isShort()) "ShortSerializer" else null
            "I" -> if (type.isInt()) "IntSerializer" else null
            "J" -> if (type.isLong()) "LongSerializer" else null
            "F" -> if (type.isFloat()) "FloatSerializer" else null
            "D" -> if (type.isDouble()) "DoubleSerializer" else null
            "C" -> if (type.isChar()) "CharSerializer" else null
            null -> null
            else -> findStandardKotlinTypeSerializerName(typeName)
        } ?: return null
        return context.getClassFromRuntimeOrNull(name, KikPackages.internalPackageFqName, KikPackages.packageFqName)
    }

    fun findStandardKotlinTypeSerializerName(typeName: String?): String? {
        return when (typeName) {
            null -> null
            "kotlin.Unit" -> "UnitSerializer"
            "kotlin.Nothing" -> "NothingSerializer"
            "kotlin.Boolean" -> "BooleanSerializer"
            "kotlin.Byte" -> "ByteSerializer"
            "kotlin.Short" -> "ShortSerializer"
            "kotlin.Int" -> "IntSerializer"
            "kotlin.Long" -> "LongSerializer"
            "kotlin.Float" -> "FloatSerializer"
            "kotlin.Double" -> "DoubleSerializer"
            "kotlin.Char" -> "CharSerializer"
            "kotlin.UInt" -> "UIntSerializer"
            "kotlin.ULong" -> "ULongSerializer"
            "kotlin.UByte" -> "UByteSerializer"
            "kotlin.UShort" -> "UShortSerializer"
            "kotlin.String" -> "StringSerializer"
            "kotlin.Pair" -> "PairSerializer"
            "kotlin.Triple" -> "TripleSerializer"
            "kotlin.collections.Collection", "kotlin.collections.List",
            "kotlin.collections.ArrayList", "kotlin.collections.MutableList" -> "ArrayListSerializer"

            "kotlin.collections.Set", "kotlin.collections.LinkedHashSet", "kotlin.collections.MutableSet" -> "LinkedHashSetSerializer"
            "kotlin.collections.HashSet" -> "HashSetSerializer"
            "kotlin.collections.Map", "kotlin.collections.LinkedHashMap", "kotlin.collections.MutableMap" -> "LinkedHashMapSerializer"
            "kotlin.collections.HashMap" -> "HashMapSerializer"
            "kotlin.collections.Map.Entry" -> "MapEntrySerializer"
            "kotlin.ByteArray" -> "ByteArraySerializer"
            "kotlin.ShortArray" -> "ShortArraySerializer"
            "kotlin.IntArray" -> "IntArraySerializer"
            "kotlin.LongArray" -> "LongArraySerializer"
            "kotlin.UByteArray" -> "UByteArraySerializer"
            "kotlin.UShortArray" -> "UShortArraySerializer"
            "kotlin.UIntArray" -> "UIntArraySerializer"
            "kotlin.ULongArray" -> "ULongArraySerializer"
            "kotlin.CharArray" -> "CharArraySerializer"
            "kotlin.FloatArray" -> "FloatArraySerializer"
            "kotlin.DoubleArray" -> "DoubleArraySerializer"
            "kotlin.BooleanArray" -> "BooleanArraySerializer"
            "kotlin.time.Duration" -> "DurationSerializer"
            "kotlin.uuid.Uuid" -> "UuidSerializer"
            "java.lang.Boolean" -> "BooleanSerializer"
            "java.lang.Byte" -> "ByteSerializer"
            "java.lang.Short" -> "ShortSerializer"
            "java.lang.Integer" -> "IntSerializer"
            "java.lang.Long" -> "LongSerializer"
            "java.lang.Float" -> "FloatSerializer"
            "java.lang.Double" -> "DoubleSerializer"
            "java.lang.Character" -> "CharSerializer"
            "java.lang.String" -> "StringSerializer"
            "java.util.Collection", "java.util.List", "java.util.ArrayList" -> "ArrayListSerializer"
            "java.util.Set", "java.util.LinkedHashSet" -> "LinkedHashSetSerializer"
            "java.util.HashSet" -> "HashSetSerializer"
            "java.util.Map", "java.util.LinkedHashMap" -> "LinkedHashMapSerializer"
            "java.util.HashMap" -> "HashMapSerializer"
            "java.util.Map.Entry" -> "MapEntrySerializer"
            else -> return null
        }
    }

    companion object {
        fun generate(
            irClass: IrClass,
            context: KikPluginContext,
        ) {
            val serializableClass = getSerializableClassByCompanion(irClass) ?: return
            if (serializableClass.shouldHaveGeneratedMethodsInCompanion) {
                SerializableCompanionIrGenerator(irClass, serializableClass, context).generate()
                irClass.addDefaultConstructorBodyIfAbsent(context)
                irClass.patchDeclarationParents(irClass.parent)
            }
        }
    }
}
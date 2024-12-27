// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k1

import org.erwinkok.kik.compiler.resolve.KikPackages
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class KikPluginContext(baseContext: IrPluginContext) : IrPluginContext by baseContext {
    internal fun getClassFromRuntime(className: String, vararg packages: FqName): IrClassSymbol {
        return getClassFromRuntimeOrNull(className, *packages) ?: error(
            "Class $className wasn't found in ${packages.toList().ifEmpty { KikPackages.allPublicPackages }}. " +
                    "Check that you have correct version of serialization runtime in classpath."
        )
    }

    internal fun getClassFromRuntimeOrNull(className: String, vararg packages: FqName): IrClassSymbol? {
        val listToSearch = if (packages.isEmpty()) KikPackages.allPublicPackages else packages.toList()
        for (pkg in listToSearch) {
            referenceClassId(ClassId(pkg, Name.identifier(className)))?.let { return it }
        }
        return null
    }

    internal fun getClassFromInternalSerializationPackage(className: String): IrClassSymbol =
        getClassFromRuntimeOrNull(className, KikPackages.internalPackageFqName)
            ?: error("Class $className wasn't found in ${KikPackages.internalPackageFqName}. Check that you have correct version of serialization runtime in classpath.")

    internal fun referenceClassId(classId: ClassId): IrClassSymbol? = referenceClass(classId)
}
// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2.checkers

import org.erwinkok.kik.compiler.k2.hasKikAnnotation
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.StandardClassIds

internal object FirKikPluginClassChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.symbol.hasKikAnnotation(context.session)) {
            return
        }
        val checkers = listOf(
            ::checkSuperClass,
            ::checkObject,
            ::checkAnonymousClass,
            ::checkInnerClass,
            ::checkTypeParameters,
        )
        checkers.forEach { checker ->
            if (checker(declaration, context, reporter)) {
                return
            }
        }
    }

    private fun checkSuperClass(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        val classSymbol = declaration.symbol
        val superClasses = classSymbol.resolvedSuperTypes
            .mapNotNull {
                it.classId
            }
            .filter {
                it != StandardClassIds.Any && it != StandardClassIds.Enum
            }
        if (superClasses.isNotEmpty()) {
            val identifiers = superClasses.joinToString(", ") { it.asFqNameString() }
            reporter.reportOn(classSymbol.source, FirKikErrors.SUPERCLASS_NOT_SUPPORTED, identifiers, context)
            return true
        }
        return false
    }

    private fun checkObject(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        val classSymbol = declaration.symbol
        if (classSymbol.classKind.isObject) {
            reporter.reportOn(declaration.source, FirKikErrors.OBJECTS_NOT_SUPPORTED, context)
            return true
        }
        return false
    }

    private fun checkAnonymousClass(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        val classSymbol = declaration.symbol
        if (classSymbol is FirAnonymousObjectSymbol || context.containingDeclarations.any { it is FirAnonymousObject }) {
            reporter.reportOn(declaration.source, FirKikErrors.ANONYMOUS_OBJECTS_NOT_SUPPORTED, context)
            return true
        }
        return false
    }

    private fun checkInnerClass(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        val classSymbol = declaration.symbol
        if (classSymbol.isInner) {
            reporter.reportOn(declaration.source, FirKikErrors.INNER_CLASSES_NOT_SUPPORTED, context)
            return true
        }
        return false
    }

    private fun checkTypeParameters(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        if (declaration.typeParameters.isNotEmpty()) {
            val identifiers = declaration.typeParameters.joinToString(", ") { it.symbol.name.identifier }
            reporter.reportOn(declaration.source, FirKikErrors.TYPE_PARAMETERS_NOT_SUPPORTED, identifiers, context)
            return true
        }
        return false
    }
}

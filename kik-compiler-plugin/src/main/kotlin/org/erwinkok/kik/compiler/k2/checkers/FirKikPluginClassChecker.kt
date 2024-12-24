// Copyright (c) 2024. Erwin Kok. Apache License. See LICENSE file for more details.
package org.erwinkok.kik.compiler.k2.checkers

import org.erwinkok.kik.compiler.k2.FirKikProperties
import org.erwinkok.kik.compiler.k2.FirKikProperty
import org.erwinkok.kik.compiler.k2.getKikPropertyNameAnnotation
import org.erwinkok.kik.compiler.k2.getPropertyNameValue
import org.erwinkok.kik.compiler.k2.hasKikAnnotation
import org.erwinkok.kik.compiler.k2.isTypeParameter
import org.erwinkok.kik.compiler.k2.kikAnnotationSource
import org.erwinkok.kik.compiler.k2.services.kikPropertiesProvider
import org.erwinkok.kik.compiler.resolve.KikEntityNames.kikCommonTypeClassId
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.isNonPrimitiveArray
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.StandardClassIds

internal object FirKikPluginClassChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        checkEnum(declaration.symbol, context, reporter)

        if (!declaration.symbol.hasKikAnnotation(context.session)) {
            return
        }
        val classCheckers = listOf(
            ::checkSuperClass,
            ::checkObject,
            ::checkAnonymousClass,
            ::checkInnerClass,
            ::checkAbstractClass,
            ::checkTypeParameters,
            ::checkCompanion,
        )
        classCheckers.forEach { checker ->
            if (checker(declaration, context, reporter)) {
                return
            }
        }
        val classSymbol = declaration.symbol
        if (classSymbol is FirRegularClassSymbol) {
            val properties = buildSerializableProperties(classSymbol, context, reporter)
            if (properties != null) {
                analyzePropertiesSerializers(properties.kikProperties, context, reporter)
            }
        }
    }

    private fun checkEnum(classSymbol: FirClassSymbol<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!classSymbol.isEnumClass) {
            return
        }
        val entryBySerialName = mutableMapOf<String, FirEnumEntrySymbol>()
        for (enumEntrySymbol in classSymbol.collectEnumEntries()) {
            val serialNameAnnotation = enumEntrySymbol.getKikPropertyNameAnnotation(context.session)
            val serialName = enumEntrySymbol.getPropertyNameValue(context.session) ?: enumEntrySymbol.name.asString()
            val firstEntry = entryBySerialName[serialName]
            if (firstEntry != null) {
                val source = serialNameAnnotation?.source ?: firstEntry.getKikPropertyNameAnnotation(context.session)?.source ?: enumEntrySymbol.source
                reporter.reportOn(
                    source,
                    FirKikErrors.DUPLICATE_PROPERTY_NAME_ENUM,
                    classSymbol,
                    serialName,
                    enumEntrySymbol.name.asString(),
                    context
                )
            } else {
                entryBySerialName[serialName] = enumEntrySymbol
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
                it != StandardClassIds.Any && it != StandardClassIds.Enum && it.outermostClassId != kikCommonTypeClassId
            }
        if (superClasses.isNotEmpty()) {
            val identifiers = superClasses.joinToString(", ") { it.asFqNameString() }
            reporter.reportOn(classSymbol.kikAnnotationSource(context.session), FirKikErrors.SUPERCLASS_NOT_SUPPORTED, identifiers, context)
            return true
        }
        return false
    }

    private fun checkObject(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        val classSymbol = declaration.symbol
        if (classSymbol.classKind.isObject) {
            reporter.reportOn(classSymbol.kikAnnotationSource(context.session), FirKikErrors.OBJECTS_NOT_SUPPORTED, context)
            return true
        }
        return false
    }

    private fun checkAnonymousClass(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        val classSymbol = declaration.symbol
        if (classSymbol is FirAnonymousObjectSymbol || context.containingDeclarations.any { it is FirAnonymousObject }) {
            reporter.reportOn(classSymbol.kikAnnotationSource(context.session), FirKikErrors.ANONYMOUS_OBJECTS_NOT_SUPPORTED, context)
            return true
        }
        return false
    }

    private fun checkInnerClass(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        val classSymbol = declaration.symbol
        if (classSymbol.isInner) {
            reporter.reportOn(classSymbol.kikAnnotationSource(context.session), FirKikErrors.INNER_CLASSES_NOT_SUPPORTED, context)
            return true
        }
        return false
    }

    private fun checkAbstractClass(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        val classSymbol = declaration.symbol
        if (classSymbol.isAbstract) {
            reporter.reportOn(classSymbol.kikAnnotationSource(context.session), FirKikErrors.ABSTRACT_CLASSES_NOT_SUPPORTED, context)
            return true
        }
        return false
    }

    private fun checkTypeParameters(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        val classSymbol = declaration.symbol
        if (declaration.typeParameters.isNotEmpty()) {
            val identifiers = declaration.typeParameters.joinToString(", ") { it.symbol.name.identifier }
            reporter.reportOn(classSymbol.kikAnnotationSource(context.session), FirKikErrors.TYPE_PARAMETERS_NOT_SUPPORTED, identifiers, context)
            return true
        }
        return false
    }

    private fun checkCompanion(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter): Boolean {
        val classSymbol = declaration.symbol
        if (classSymbol !is FirRegularClassSymbol) return false
        val companionObjectSymbol = classSymbol.companionObjectSymbol
        if (companionObjectSymbol != null) {
            reporter.reportOn(classSymbol.kikAnnotationSource(context.session), FirKikErrors.COMPANION_OBJECT_NOT_SUPPORTED, companionObjectSymbol.name.identifier, context)
            return true
        }
        return false
    }

    private fun buildSerializableProperties(classSymbol: FirRegularClassSymbol, context: CheckerContext, reporter: DiagnosticReporter): FirKikProperties? {
        val properties = context.session.kikPropertiesProvider.getKikPropertiesForClass(classSymbol)
        // check that all names are unique
        val namesSet = mutableSetOf<String>()
        for (property in properties.kikProperties) {
            val name = property.name
            if (!namesSet.add(name)) {
                reporter.reportOn(classSymbol.kikAnnotationSource(context.session), FirKikErrors.DUPLICATE_PROPERTY_NAME, name, context)
            }
        }
        return properties
    }

    private fun analyzePropertiesSerializers(
        properties: List<FirKikProperty>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (property in properties) {
            val propertySymbol = property.propertySymbol
            val typeRef = propertySymbol.resolvedReturnTypeRef
            val propertyType = typeRef.coneType.fullyExpandedType(context.session)
            val source = typeRef.source ?: propertySymbol.source
            checkTypeParams(propertyType, source, propertySymbol, context, reporter)
            checkGenericArrayType(propertyType, source, context, reporter)
        }
    }

    private fun checkTypeParams(
        propertyType: ConeKotlinType,
        typeSource: KtSourceElement?,
        propertySymbol: FirPropertySymbol,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (propertyType.typeArguments.isNotEmpty()) {
            reporter.reportOn(
                typeSource,
                FirKikErrors.PROPERTY_TYPE_PARAMETER_NOT_SUPPORTED,
                propertySymbol,
                context
            )
        }
    }

    private fun checkGenericArrayType(
        propertyType: ConeKotlinType,
        source: KtSourceElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (propertyType.isNonPrimitiveArray && propertyType.typeArguments.first().type?.isTypeParameter == true) {
            reporter.reportOn(
                source,
                FirKikErrors.GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED,
                context
            )
        }
    }
}

package org.erwinkok.kik.compiler.gradleplugin

enum class ErrorCheckingMode {
    NONE,
    WARNING,
    ERROR,
}

open class KikCompilerGradleConfiguration {
    var generateQualifiedTypeName: Boolean = false
    var errorCheckingMode: ErrorCheckingMode = ErrorCheckingMode.ERROR
}

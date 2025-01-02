package org.erwinkok.kik.compiler.gradleplugin

enum class ErrorCheckingMode {
    NONE,
    WARNING,
    ERROR,
}

open class KikCompilerGradleConfiguration {
    var enabled: Boolean = true
    var errorCheckingMode: ErrorCheckingMode = ErrorCheckingMode.ERROR
}

@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Версию Binaryen (wasm-opt) можно сменить параметром -PbinaryenVersion=124.
// Без параметра остаётся версия Kotlin по умолчанию.
plugins.withType<BinaryenPlugin> {
    providers.gradleProperty("binaryenVersion").orNull?.let { v ->
        the<BinaryenEnvSpec>().version.set(v)
    }
}

// wasm-opt падает на CI с кодом 139 (на Binaryen 124 и 125). Режим оптимизации задаётся параметром
// -PwasmOptMode: full (по умолчанию в Kotlin), light (только -O2), none (без оптимизаций).
// Общие флаги функций те же, что в Kotlin (BinaryenConfig), меняется только набор проходов оптимизации.
providers.gradleProperty("wasmOptMode").orNull?.let { mode ->
    val commonArgs = listOf(
        "--enable-gc",
        "--enable-reference-types",
        "--enable-exception-handling",
        "--enable-bulk-memory",
        "--enable-nontrapping-float-to-int",
        "--no-inline=kotlin.wasm.internal.throwValue",
        "--no-inline=kotlin.wasm.internal.getKotlinException",
        "--no-inline=kotlin.wasm.internal.jsToKotlinStringAdapter",
        "--inline-functions-with-loops",
        "--traps-never-happen",
        "--fast-math",
    )
    val passes: List<String>? = when (mode) {
        "none" -> emptyList()
        "light" -> listOf("-O2")
        else -> null
    }
    if (passes != null) {
        tasks.withType<BinaryenExec>().configureEach {
            binaryenArguments.set(commonArgs + passes)
        }
    }
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "autospace-web"
        browser {
            commonWebpackConfig {
                outputFileName = "autospace-web.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":app:shared"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
        }
    }
}
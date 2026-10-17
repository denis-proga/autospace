@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Версию Binaryen (wasm-opt) можно сменить параметром -PbinaryenVersion=124.
// Без параметра остаётся версия Kotlin по умолчанию. CI передаёт параметр, потому что
// wasm-opt 125 на Linux падает с кодом 139 (segmentation fault).
plugins.withType<BinaryenPlugin> {
    providers.gradleProperty("binaryenVersion").orNull?.let { v ->
        the<BinaryenEnvSpec>().version.set(v)
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
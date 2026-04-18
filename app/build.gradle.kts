plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "android.zero.studio.compose.preview"
    compileSdk = 36

    defaultConfig {
        applicationId = "android.zero.studio.compose.preview"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    packaging {
        resources {
            pickFirsts += "kotlin/coroutines/coroutines.kotlin_builtins"
            pickFirsts += "kotlin/annotation/annotation.kotlin_builtins"
            pickFirsts += "kotlin/concurrent/atomics/atomics.kotlin_builtins"
        }
    }
    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    // 代码编辑器核心
    implementation(platform("io.github.rosemoe:editor-bom:0.24.4"))
    implementation("io.github.rosemoe:editor")
    implementation("io.github.rosemoe:language-textmate")
    implementation("io.github.rosemoe:language-treesitter")
    api(files("libs/kotlinc-embeddable.jar"))
    api(files("libs/library.jar"))

    // AndroidX 核心 UI 库
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.preference.ktx)

    // 架构组件
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Jetpack Compose 运行时与渲染支持 (用于 EditorFragment 的 ComposeView)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.google.r8)
    implementation(libs.utilcodex)
    implementation(libs.asm)
    implementation(libs.dexlib2)
}

tasks.whenTaskAdded {
    // 只要执行 App 的资产打包生成步骤，就自动先去运行我们的插件脱壳重组任务
    if (name == "generateDebugAssets" || name == "generateReleaseAssets") {
        dependsOn(":plugin-dexer:dexComposePlugin")
    }
}

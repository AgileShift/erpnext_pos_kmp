import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.io.FileInputStream
import java.util.Properties

val properties = Properties()
try {
    properties.load(FileInputStream(rootProject.file("gradle.properties")))
} catch (e: Exception) {
    e.printStackTrace()
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.build.konfig)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.google.services)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(), iosArm64(), iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sqldelight {
        databases {
            create("ERPNextPos") {
                packageName = "com.erpnext.pos"
            }
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(libs.bignum)

            implementation(libs.pullrefresh)

            implementation(libs.androidx.paging.compose)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)

            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.paging)
            // implementation(libs.androidx.room.ktx)
            implementation(libs.androidx.sqlite.bundled)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.plugin.auth)
            implementation(libs.ktor.plugin.logging)

            implementation(libs.koin.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.sqldelight.coroutines)

            implementation(libs.bundles.coil)

            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            // Firebase
            implementation(libs.firebase.analytics)
            implementation(project.dependencies.platform(libs.firebase.bom))

            implementation(libs.sqldelight.android)
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.core.ktx)

            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)

            implementation(libs.security.crypto)
            implementation(libs.android.tink)

            implementation(libs.androidx.room.paging)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqldelight.jvm)

            implementation(libs.java.keyring)
            implementation(libs.logback.classic)
        }
        iosMain.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation(libs.sqldelight.native)
            implementation(libs.ktor.client.darwin)
        }
    }

    sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/metadata")
    }
}

buildkonfig {
    packageName = "com.erpnext.pos"

    // default config is required
    defaultConfigs {
        buildConfigField(STRING, "BASE_URL", properties["BASE_URL"].toString().replace("\"", ""))
        buildConfigField(STRING, "CLIENT_ID", properties["CLIENT_ID"].toString().replace("\"", ""))
        buildConfigField(
            STRING, "CLIENT_SECRET", properties["CLIENT_SECRET"].toString().replace("\"", "")
        )
        buildConfigField(
            STRING, "REDIRECT_URI", properties["REDIRECT_URL"].toString().replace("\"", "")
        )
        buildConfigField(
            STRING,
            "DESKTOP_REDIRECT_URI",
            properties["DESKTOP_REDIRECT_URL"].toString().replace("\"", "")
        )
        buildConfigField(
            STRING,
            "DESKTOP_CLIENT_ID",
            properties["DESKTOP_CLIENT_ID"].toString().replace("\"", "")
        )
        buildConfigField(
            STRING,
            "DESKTOP_CLIENT_SECRET",
            properties["DESKTOP_CLIENT_SECRET"].toString().replace("\"", "")
        )

    }

    defaultConfigs("staging") {
        buildConfigField(STRING, "BASE_URL", properties["BASE_URL"].toString().replace("\"", ""))
        buildConfigField(STRING, "CLIENT_ID", properties["CLIENT_ID"].toString().replace("\"", ""))
        buildConfigField(
            STRING, "CLIENT_SECRET", properties["CLIENT_SECRET"].toString().replace("\"", "")
        )
        buildConfigField(
            STRING, "REDIRECT_URI", properties["REDIRECT_URL"].toString().replace("\"", "")
        )
    }
}

android {
    namespace = "com.erpnext.pos"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.erpnext.pos"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

compose.desktop {
    application {
        mainClass = "com.erpnext.pos.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.erpnext.pos"
            packageVersion = "1.0.0"
        }
    }
    dependencies {
        debugImplementation(compose.uiTooling)

        ksp(libs.androidx.room.compiler)
    }
}
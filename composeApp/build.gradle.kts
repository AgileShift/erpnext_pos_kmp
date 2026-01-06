import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

val properties = Properties()
try {
    properties.load(FileInputStream(rootProject.file("gradle.properties")))
} catch (e: Exception) {
    e.printStackTrace()
}

compose.desktop {
    application {
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi, TargetFormat.Dmg)
        }
    }
}
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.build.konfig)
    alias(libs.plugins.sqlDelight)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidLibrary {
        namespace = "com.erpnext.pos.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

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

sqldelight {
    databases {
        create("ERPNextPos") {
            packageName = "com.erpnext.pos"
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
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

        buildTypes {
            release {
                proguard {
                    configurationFiles.from(file("proguard-desktop.pro"))
                }
            }
        }
    }
}

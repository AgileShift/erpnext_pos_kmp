import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.detekt)
  alias(libs.plugins.google.services)
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

  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures { compose = true }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

detekt {
  buildUponDefaultConfig = true
  allRules = false
  ignoreFailures = true
  config.setFrom(rootProject.file("config/detekt/detekt.yml"))
  baseline = file("detekt-baseline.xml")
  source.setFrom("src/main/kotlin")
}

tasks.withType<Detekt>().configureEach {
  jvmTarget = "11"
  reports {
    html.required.set(true)
    sarif.required.set(true)
    xml.required.set(true)
    txt.required.set(false)
  }
}

dependencies {
  implementation(projects.composeApp)
  implementation(libs.ui.tooling)
  implementation(libs.ui.tooling.preview)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.work.runtime.ktx)
  implementation(libs.koin.android)
  implementation(libs.androidx.lifecycle.process)

  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.analytics)
  implementation(libs.androidx.material3)
}

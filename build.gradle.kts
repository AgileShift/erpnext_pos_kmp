plugins {
  alias(libs.plugins.spotless)
  alias(libs.plugins.detekt) apply false
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.kmpLibrary) apply false
  alias(libs.plugins.composeHotReload) apply false
  alias(libs.plugins.composeMultiplatform) apply false
  alias(libs.plugins.composeCompiler) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.androidx.room) apply false
  alias(libs.plugins.kotlinSerialization) apply false
  alias(libs.plugins.google.services) apply false
}

spotless {
  kotlin {
    target("**/*.kt")
    targetExclude("**/build/**")
    ktfmt()
  }

  kotlinGradle {
    target("**/*.gradle.kts")
    targetExclude("**/build/**")
    ktfmt()
  }
}

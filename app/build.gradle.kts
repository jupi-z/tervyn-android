plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.ksp)
}

val remoteApiEnabledProvider = providers.gradleProperty("TERVYN_REMOTE_API_ENABLED")
  .orElse(providers.environmentVariable("TERVYN_REMOTE_API_ENABLED"))
  .orElse("false")

val remoteApiBaseUrlProvider = providers.gradleProperty("TERVYN_API_BASE_URL")
  .orElse(providers.environmentVariable("TERVYN_API_BASE_URL"))
  .orElse("https://tervyn.invalid/")

fun String.asBuildConfigStringLiteral(): String =
  "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
  namespace = "dev.amenokizele.tervyn"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "dev.amenokizele.tervyn"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField(
      "boolean",
      "TERVYN_REMOTE_API_ENABLED",
      remoteApiEnabledProvider.map { it.toBooleanStrictOrNull() ?: false }.get().toString()
    )
    buildConfigField(
      "String",
      "TERVYN_API_BASE_URL",
      remoteApiBaseUrlProvider.get().asBuildConfigStringLiteral()
    )
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    isCoreLibraryDesugaringEnabled = true
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  sourceSets["androidTest"].assets.directories.add("$projectDir/schemas")
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

configurations.configureEach {
  if (name.contains("AndroidTest", ignoreCase = true)) {
    // Room migration Android tests load schema JSON with serializers compiled
    // against kotlinx-serialization 1.8.x; force AndroidTest runtime alignment.
    resolutionStrategy.force(
      "org.jetbrains.kotlinx:kotlinx-serialization-bom:1.8.1",
      "org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1",
      "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1",
      "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1",
      "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1",
    )
  }
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
  arg("room.incremental", "true")
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.hilt.android)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.kotlinx.serialization)
  implementation(libs.okhttp.core)
  implementation(libs.okhttp.logging.interceptor)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  ksp(libs.hilt.compiler)
  ksp(libs.androidx.room.compiler)
  coreLibraryDesugaring(libs.desugar.jdk.libs)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.okhttp.mockwebserver)

  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation(libs.kotlinx.serialization.json)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.navigation.testing)
  androidTestImplementation(libs.androidx.room.testing)
}

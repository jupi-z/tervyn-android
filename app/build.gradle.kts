plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.ksp)
}

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

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  ksp(libs.hilt.compiler)
  ksp(libs.androidx.room.compiler)
  coreLibraryDesugaring(libs.desugar.jdk.libs)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation(libs.kotlinx.serialization.json)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.room.testing)
}
